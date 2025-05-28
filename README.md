[![REUSE status](https://api.reuse.software/badge/github.com/cap-java/cds-feature-advanced-event-mesh)](https://api.reuse.software/info/github.com/cap-java/cds-feature-advanced-event-mesh)
[![Main build and deploy](https://github.com/cap-java/cds-feature-advanced-event-mesh/actions/workflows/main-build.yml/badge.svg)](https://github.com/cap-java/cds-feature-advanced-event-mesh/actions/workflows/main-build.yml)
[![Deploy to Artifactory](https://github.com/cap-java/cds-feature-advanced-event-mesh/actions/workflows/main-build-and-deploy.yml/badge.svg)](https://github.com/cap-java/cds-feature-advanced-event-mesh/actions/workflows/main-build-and-deploy.yml)

# cds-feature-advanced-event-mesh



## About this project

CDS plugin providing integration with SAP Integration Suite, advanced event mesh.



## Table of Contents

- [About this project](#about-this-project)
- [Requirements](#requirements)
- [Setup](#setup)
  - [Setting Up the Broker](#setting-up-the-broker)
  - [Broker Validation](#broker-validation)
- [Support, Feedback, Contributing](#support-feedback-contributing)
- [Security / Disclosure](#security--disclosure)
- [Code of Conduct](#code-of-conduct)
- [Licensing](#licensing)



## Requirements

See [Getting Started](https://cap.cloud.sap/docs/get-started/in-a-nutshell?impl-variant=java) on how to jumpstart your development and grow as you go with SAP Cloud Application Programming Model (CAP).
To learn about messaging in CAP, please consult the guide on [Events & Messaging](https://cap.cloud.sap/docs/guides/messaging/).



## Setup

Install the plugin by adding it to the dependencies of your CAP Java project:

```xml
<!-- srv/pom.xml -->
<dependency>
  <groupId>com.sap.cds</groupId>
  <artifactId>cds-feature-advanced-event-mesh</artifactId>
  <version>${latest-version}</version>
</dependency>
```

The latest version can be found in the [changelog](./CHANGELOG.md) or in the [Maven Central Repository](https://central.sonatype.com/artifact/com.sap.cds/cds-feature-advanced-event-mesh/versions).
The usage of CAP Java plugins is described in the [CAP Java Documentation](https://cap.cloud.sap/docs/java/building-plugins#reference-the-new-cds-model-in-an-existing-cap-java-project).

Then, set the `kind` of your messaging service to `aem`:

```yaml
# srv/src/main/resources/application.yaml
cds:
  messaging.services:
  - name: "messaging-name"
    kind: "aem"
```


### Setting Up the Broker

The broker itself must be created manually in SAP Integration Suite, advanced event mesh and trust must be established to the respective application in [SAP Cloud Identity Services](https://help.sap.com/docs/cloud-identity-services/cloud-identity-services), both for the Solace broker and the [SEMP API](https://docs.solace.com/Admin/SEMP/Using-SEMP.htm).
For details, please consult SAP Integration Suite, advanced event mesh's documentation at [help.pubsub.em.services.cloud.sap](https://help.pubsub.em.services.cloud.sap/Get-Started/get-started-lp.htm).

// TODO: where to put?
You need to manually configure SAP Integration Suite, advanced event mesh to allow your application to connect to the broker by following the [steps in the documentation](https://help.sap.com/docs/sap-integration-suite/advanced-event-mesh/cap-plugin-for-sap-integration-suite-advanced-event-mesh).

The broker's credentials must be provided via a [user-provided service instance](https://docs.cloudfoundry.org/devguide/services/user-provided.html) with the name `advanced-event-mesh` and credentials in the following format:

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
      "smf_uri": "wss://<broker host>:<port>"
    }
  },
  "vpn": "<vpn>"
}
```

To troubleshoot connection issues, set log level for component `messaging` to `DEBUG`.
Check [`cds.log()`](https://cap.cloud.sap/docs/node.js/cds-log) for how to maintain log levels.


### Broker Validation

Your app must be bound to an instance of service `SAP Integration Suite, advanced event mesh` with plan `aem-validation-service`.
Please see [Validation of VMR Provisioning](https://help.sap.com/docs/sap-integration-suite/advanced-event-mesh/validation-of-vmr-provisioning) for more information.



## Support, Feedback, Contributing

This project is open to feature requests/suggestions, bug reports etc. via [GitHub issues](https://github.com/cap-java/cds-feature-advanced-event-mesh/issues). Contribution and feedback are encouraged and always welcome. For more information about how to contribute, the project structure, as well as additional contribution information, see our [Contribution Guidelines](CONTRIBUTING.md).



## Security / Disclosure

If you find any bug that may be a security problem, please follow our instructions at [in our security policy](https://github.com/cap-java/cds-feature-advanced-event-mesh/security/policy) on how to report it. Please do not create GitHub issues for security-related doubts or problems.



## Code of Conduct

We as members, contributors, and leaders pledge to make participation in our community a harassment-free experience for everyone. By participating in this project, you agree to abide by its [Code of Conduct](https://github.com/cap-java/.github/blob/main/CODE_OF_CONDUCT.md) at all times.



## Licensing

Copyright 2025 SAP SE or an SAP affiliate company and cds-feature-advanced-event-mesh contributors. Please see our [LICENSE](LICENSE) for copyright and license information. Detailed information including third-party components and their licensing/copyright information is available [via the REUSE tool](https://api.reuse.software/info/github.com/cap-java/cds-feature-advanced-event-mesh).
