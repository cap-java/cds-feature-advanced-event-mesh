---
status: draft
priority: low
created: 2026-06-29
---

# Example: surface broker reconnection events as CAP events

This file is an example of the convention. Copy it to start a real idea
and edit accordingly.

## Problem

When the underlying SAP Advanced Event Mesh connection drops and
reconnects, applications using this plugin have no programmatic way to
react — no warm cache to flush, no metric to bump, no log to emit. They
only see it indirectly via failed publish attempts.

## Proposed behavior

Emit a CAP event on the `MessagingService` whenever the broker connection
state changes: `connection.up`, `connection.down`, `connection.reconnect`.
Applications subscribe with `@On(event = "connection.down", service = "messaging")`.

## API sketch

```java
// New constants in MessagingEvents
public static final String CONNECTION_UP      = "connection.up";
public static final String CONNECTION_DOWN    = "connection.down";
public static final String CONNECTION_RECONNECT = "connection.reconnect";

// Event payload
public record ConnectionStateEvent(
    String reason,        // human-readable cause, e.g. "heartbeat timeout"
    Instant timestamp,
    int attemptNumber     // 0 for first connect; n for nth reconnect attempt
) {}
```

## Acceptance criteria

- [ ] `ConnectionStateEvent` payload class exists with those three fields.
- [ ] On JMS `ConnectionListener.onException`, plugin emits `connection.down`.
- [ ] On successful reconnect, plugin emits `connection.up` (initial) or
      `connection.reconnect` (subsequent).
- [ ] Unit test verifies an `@On(connection.down)` handler is invoked.
- [ ] README has a 10-line "Reacting to connection events" section.

## Out of scope

- Programmatic reconnection control (`forceReconnect()`). Connection
  state is observable, not steerable.
- Per-queue connection events. This is broker-level only.

## References

- JMS spec on `ExceptionListener`
- Existing `MessagingService` event constants in
  `cds-feature-advanced-event-mesh/src/main/java/.../MessagingEvents.java`
