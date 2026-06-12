# `cds-feature-advanced-event-mesh` — Architecture & Design

This document explains how the `cds-feature-advanced-event-mesh` plugin is structured, how it integrates with the CAP Java messaging core (`cds-services-messaging`), and how it differs from its closest sibling, `cds-feature-enterprise-messaging`.

---

## TL;DR

This is a thin (~1.1k LoC) CAP Java plugin that plugs **SAP Integration Suite, Advanced Event Mesh** (a managed Solace PubSub+ broker) into CAP's generic messaging service abstraction. It does this by:

1. Implementing the abstract methods of `AbstractMessagingService` (from `cds-services-messaging` core).
2. Providing AMQP/JMS connectivity via Qpid + a custom `BrokerConnectionProvider` that does OAuth2 (XOAUTH2 SASL) with token rotation.
3. Providing a REST (SEMP v2) management client to create/delete queues and queue→topic subscriptions on the Solace broker.
4. Calling a separate AEM "validation service" once before the first publish to prove the broker is provisioned for this subaccount.

Structurally it is almost identical to `cds-feature-enterprise-messaging` — that plugin was the template.

---

## 1. Position in the CAP Java messaging stack

The CAP Java messaging story (in `cds-services/repo-cds-services/cds-services-messaging`) is built around three core types that any broker plugin must satisfy:

- **`AbstractMessagingService`** (`cds-services-messaging/.../service/AbstractMessagingService.java:34`) — owns the lifecycle (`init` / `stop`), the CAP event-handler integration (`@On` / `@Before` for `TopicMessageEventContext`), the auto-completion of known topics, optional CloudEvents wrapping, and the queue/subscription bootstrap (`createOrUpdateQueuesAndSubscriptions`, line 87). It declares the abstract methods every broker must implement: `removeQueue`, `createQueue`, `createQueueSubscription`, `registerQueueListener`, `emitTopicMessage`.
- **`BrokerConnection`** (`.../jms/BrokerConnection.java:24`) — wraps a JMS `Connection` plus a `MessageEmitter`, and registers `MessageQueueReader`s. Owns the auto-reconnect timer with exponential backoff (2 → 10 min).
- **`BrokerConnectionProvider`** (`.../jms/BrokerConnectionProvider.java:21`) — abstract factory with one method `createBrokerConnection`. Provides connection sharing across services with the same client config (`configuredConnections` map) and asynchronous initialization on a daemon thread.

The AEM plugin extends `AbstractMessagingService` and `BrokerConnectionProvider`; everything else (event dispatching, retries, outboxing, reconnect) is reused unchanged.

---

## 2. Module map

```
cds-feature-advanced-event-mesh/src/main/java/com/sap/cds/feature/messaging/aem/
├── service/
│   ├── AemMessagingServiceConfiguration.java   ← entry point (SPI)
│   └── AemMessagingService.java                ← extends AbstractMessagingService
├── jms/
│   └── AemMessagingConnectionProvider.java     ← extends BrokerConnectionProvider
└── client/
    ├── RestClient.java                         ← shared HTTP helper (Apache httpclient4)
    ├── AemManagementClient.java                ← SEMP v2 calls (queues, subscriptions)
    ├── AemValidationClient.java                ← one-shot handshake call
    └── binding/
        ├── AemEndpointView.java                ← reads endpoints.advanced-event-mesh.{uri,amqp_uri}
        ├── AemAuthenticationServiceView.java   ← reads credentials.authentication-service.*
        ├── AemClientIdentity.java              ← (clientid, clientsecret) record
        ├── AemManagementOauth2PropertySupplier.java   ← OAuth2 for SEMP API
        └── AemValidationOAuth2PropertySupplier.java   ← OAuth2 for handshake API
```

The single SPI registration is in `META-INF/services/com.sap.cds.services.runtime.CdsRuntimeConfiguration` → `AemMessagingServiceConfiguration`. CAP Java discovers it on classpath at startup.

---

## 3. Boot flow (what happens when the app starts)

`AemMessagingServiceConfiguration.services()` (line 32) is called by `CdsRuntimeConfigurer`:

1. **Register OAuth2 property suppliers** with the SAP Cloud SDK destination loader so any HTTP destination built from an AEM binding automatically does the OAuth2 client-credentials dance. There are *two* suppliers because management and validation use different OAuth2 servers (one in IAS, one in BTP/XSUAA).
2. **Find bindings**: any service binding named `advanced-event-mesh` *or* tagged with that label, plus one binding for `aem-validation-service`. The validation binding is mandatory when at least one AEM broker binding is present — `createMessagingService` (called per binding, line 159) throws a `ServiceException` if it is absent. If there are no AEM broker bindings the missing validation binding is silently ignored.
3. **Per AEM binding**, decide which `MessagingServiceConfig`(s) to materialize:
   - explicit `cds.messaging.services` entries that match by binding name,
   - then by `kind: aem` / `kind: advanced-event-mesh` (only when there's exactly one binding — to avoid ambiguity),
   - else a default service.
4. Each resulting service is wrapped with `MessagingOutboxUtils.outboxed(...)` so emits go through the CAP outbox before being sent to the broker.

A single `AemMessagingConnectionProvider` is shared per binding, so all services bound to the same AEM instance reuse the same JMS connection (unless `connection.dedicated: true` is set — handled in the base `BrokerConnectionProvider`).

---

## 4. Two binding types, two roles

The plugin reads **two** distinct service bindings:

| Binding        | Tag/Name                  | Purpose                                                                  | Auth                                                            |
|----------------|---------------------------|--------------------------------------------------------------------------|-----------------------------------------------------------------|
| AEM broker     | `advanced-event-mesh`     | AMQP+JMS data plane *and* SEMP management plane                          | OAuth2 client_credentials against IAS (`authentication-service`) |
| Validation     | `aem-validation-service`  | One-time handshake confirming the VMR is provisioned for this subaccount | OAuth2 against XSUAA (`handshake.oa2`)                          |

In the test fixture (`default-env.json`) the AEM binding is `user-provided` (manually created with the right tag), while the validation binding is a real BTP service binding.

### Expected AEM binding shape

```jsonc
{
  "authentication-service": {
    "tokenendpoint": "https://<ias host>/oauth2/token",
    "clientid": "<client id>",
    "clientsecret": "<client secret>"
  },
  "endpoints": {
    "advanced-event-mesh": {
      "uri": "https://<broker host>:<port>",
      "amqp_uri": "amqps://<broker host>:<port>"
    }
  },
  "vpn": "<vpn>"
}
```

---

## 5. Connection plane (AMQP)

`AemMessagingConnectionProvider` (jms/AemMessagingConnectionProvider.java:25):

- Reads `endpoints.advanced-event-mesh.amqp_uri` from the binding and appends `/?amqp.saslMechanisms=XOAUTH2` to force OAuth-bearer SASL.
- Builds a SAP Cloud SDK `DefaultHttpDestination` so the SDK handles OAuth2 token acquisition + caching (via the registered `AemManagementOauth2PropertySupplier`). The destination is a deliberate trick — it's not actually used for HTTP, just as a token holder. `vpn` is stashed as a destination property.
- In `createBrokerConnection`, builds a Qpid `JmsConnectionFactory(vpn, "token", uri)` and registers a `PASSWORD_OVERRIDE` extension (line 89). Each time Qpid opens a connection, this lambda calls `fetchToken()` which pulls the freshly-cached `Authorization: Bearer <jwt>` header out of the SDK destination and substitutes it as the SASL password. This is the documented Solace pattern for OAuth on Qpid (https://solace.community/discussion/1677).
- The base `BrokerConnection` then handles connect/reconnect/listeners.

### Token refresh strategy (subtle)

Token refresh for AMQP relies entirely on the SAP Cloud SDK's destination-side OAuth2 caching. If the JWT expires mid-session and Qpid doesn't re-handshake, you'd see a connection error → `BrokerConnection` reconnect path → `PASSWORD_OVERRIDE` lambda fires again → fresh token. Long-lived connections survive token expiry because reconnects are routine, not because tokens are refreshed in-place.

---

## 6. Management plane (SEMP v2)

`AemManagementClient` (client/AemManagementClient.java:17) calls Solace SEMP v2 endpoints under `/SEMP/v2/config/msgVpns/{vpn}/queues`:

- `createQueue(name, properties)` — checks-then-creates, sets `permission=consume`, `ingressEnabled=true`, `egressEnabled=true`. Honors `deadMsgQueue` from properties by ensuring the DMQ exists first.
- `createQueueSubscription(queue, topic)` — checks the existing subscription list before POSTing, to be idempotent.
- `removeQueue(name)` — DELETE.

`AemMessagingService` (service/AemMessagingService.java:31) wires these into the abstract methods, with a `skipManagement` escape hatch (driven by `connection.properties.skipManagement`) that lets operators provision the broker out-of-band and have CAP just listen/publish without touching SEMP. The `subaccountId` knob is similarly threaded into the validation call.

---

## 7. Inbound topic extraction

`AbstractMessagingService` needs to map an inbound message back to a CAP topic name to dispatch the right event handler. The location of "the topic name" in the AMQP message is broker-specific:

- AEM (Solace, raw AMQP) → `getMessageTopic` reads `getDestination().getAddress()` from both `AmqpJmsTextMessageFacade` (text messages) and `AmqpJmsBytesMessageFacade` (binary messages) (lines 153–162). The destination address *is* the topic.
- EM (Solace via SAP Event Mesh) → reads `.getType()` from the same facade types instead.

The accessor is passed to `connection.registerQueueListener(queue, listener, this::getMessageTopic)` as a `TopicAccessor`, which `MessageQueueReader` uses on each delivered message.

### Outbound topic prefixing

When emitting, AEM's `emitTopicMessage` (line 144) first calls `validate()` (line 145) then passes `"topic://" + topic` to the broker (line 146); EM passes `"topic:" + topic` (single colon). Trivial wire-format difference; both Solace-flavored.

Notably, AEM does **not** override `toFullyQualifiedTopicName` or `toFullyQualifiedQueueName` like EM does. EM has a non-trivial namespace story (`$namespace` placeholders, `+/+/+/` wildcards for cross-tenant subscriptions, `.`→`/` translation for CloudEvents). AEM treats topics as opaque strings; multi-tenant fan-out and namespace conventions are the user's problem.

---

## 8. Validation handshake

`AemValidationClient.validate(managementUri, subaccountId)` POSTs `{"hostName": <host extracted from managementUri>, "subaccountId"?: ...}` to the validation service's handshake endpoint (the destination base URI from the validation binding). Only the *host* component of `managementUri` is sent, not the full URI. It runs at most once per service instance — guarded by the `aemBrokerValidated` volatile `Boolean` flag in `AemMessagingService.validate()` (line 165), and only on the first publish (lazily via `emitTopicMessage`). A 200 confirms this BTP subaccount is entitled to this VMR. Anything else → `ServiceException`, no message is sent.

This is a compliance / anti-misconfiguration check: it prevents an app from being mistakenly bound to a broker in the wrong subaccount.

---

## 9. Comparison with `cds-feature-enterprise-messaging`

| Aspect                       | `cds-feature-enterprise-messaging`                                   | `cds-feature-advanced-event-mesh`              |
|------------------------------|----------------------------------------------------------------------|------------------------------------------------|
| Underlying broker            | SAP Event Mesh (managed Solace, CF/BTP service)                      | AEM (Solace VMR in SAP Integration Suite)      |
| Binding source               | Real BTP service `enterprise-messaging`                              | User-provided service tagged `advanced-event-mesh` |
| Auth to broker (AMQP)        | clientid/secret in binding                                           | OAuth2 (IAS) → JWT → SASL XOAUTH2              |
| Management API               | EM Management API (plus webhook-based push for HTTP plan)            | Solace SEMP v2 directly                        |
| Multi-tenancy support        | Yes — webhook-per-tenant + `EnterpriseMessagingMtService`            | None                                           |
| Namespace handling           | `$namespace`, `+/+/+/` wildcards                                     | Pass-through                                   |
| Validation step              | None                                                                 | Handshake call before first emit               |
| Lines of code                | Several thousand                                                     | ~1.1k                                          |

The AEM plugin is a deliberately leaner cut — no MT, no webhook adapter, no namespacing magic — because AEM as a service is a thinner abstraction over the underlying Solace broker than EM is.

---

## 10. Configuration surface

Beyond the standard `cds.messaging.services.<name>.*` keys provided by CAP, the plugin reads:

| Property                                                            | Type      | Notes                                                                           |
|---------------------------------------------------------------------|-----------|---------------------------------------------------------------------------------|
| `cds.messaging.services.<key>.connection.properties.skipManagement` | `boolean` | If `true`, plugin will not call SEMP to create/delete queues or subscriptions   |
| `cds.messaging.services.<key>.connection.properties.subaccountId`   | `String`  | Sent as part of the validation handshake payload                                |

Both keys are also accepted in kebab-case (`skip-management`, `subaccount-id`).

---

## 11. Notes & caveats worth flagging

- `AemEndpointView.getAemEndpoint()` (line 65) takes the first value from the `endpoints` map via an iterator, then validates that the key `"advanced-event-mesh"` exists in the map. This is fragile: if a second entry is added to `endpoints` whose insertion order precedes `"advanced-event-mesh"`, the wrong endpoint value will be returned even though the key guard passes.
- `aemBrokerValidated` is declared as a boxed `volatile Boolean` (not primitive `boolean`). The check-then-act pattern in `validate()` (lines 165–175) is not synchronized, so under concurrent calls to `emitTopicMessage` the validation can be invoked multiple times before the flag is set to `true`.
- `getMessageTopic` returns `null` if it doesn't recognize the JMS message type. The `MessageQueueReader` then has to handle `null` — worth double-checking the failure path during audits.
- Two duplicate property reads (`skipManagement` vs `skip-management`, `subaccountId` vs `subaccount-id`) — supports both naming conventions.
- `AemValidationClient` posts to path `""` (empty string) because the destination URI itself is the full handshake URL — relies on Apache HttpClient resolving an empty string to the base URI.
- The plugin requires a validation binding whenever at least one AEM broker binding is present; the missing binding causes a `ServiceException` at startup per affected service instance, not at deploy time.
