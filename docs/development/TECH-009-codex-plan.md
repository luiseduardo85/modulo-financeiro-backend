# Codex Plan — TECH-009

```text
Read AGENTS.md first.

We are planning:

TECH-009 — Implement basic observability.

Before proposing changes, read:

- docs/api/errors.md
- docs/architecture/backend-architecture.md
- docs/architecture/testing.md
- docs/backlog/technical-backlog.md
- docs/backlog/issues/TECH-009-basic-observability.md

Inspect the backend after TECH-008.

Do not modify files yet.

Goal:
Introduce the minimum HTTP request-correlation and logging infrastructure required
to make the API traceId contract operational.

Scope:
- generate or resolve one traceId per HTTP request;
- store it in SLF4J MDC;
- make it available through the existing TraceIdProvider;
- clean MDC reliably after request completion;
- integrate with ErrorResponse/GlobalExceptionHandler;
- optionally expose X-Trace-Id in the response;
- document safe logging conventions;
- add focused hermetic tests.

Current assumptions:
- TECH-006 already introduced TraceIdProvider;
- the API error contract already contains traceId;
- traceId may currently be null because no request correlation mechanism exists.

Important decisions to analyze:

1. Incoming trace header
Determine whether TECH-009 should accept an incoming X-Trace-Id.

Do not blindly trust arbitrary client-provided values.

If accepting it, define:
- allowed format;
- maximum length;
- fallback behavior.

If there is no strong need yet, prefer generating a server-side ID.

2. Response header
Evaluate returning:

X-Trace-Id: <traceId>

Prefer this if it improves supportability without introducing security risk.

3. ID format
Prefer a simple UUID generated locally.
Do not add a dependency only for ID generation.

4. MDC
Use SLF4J MDC.

Official key:

traceId

Centralize:
- MDC key;
- response/request header name if used.

Do not duplicate string literals.

5. Lifecycle
The trace context must be cleaned in a finally block or equivalent reliable lifecycle hook.

One request must never leak MDC values into another request/thread.

6. Logging
Establish minimum safe conventions:
- unexpected 500 errors log server-side stack trace;
- traceId is included when present;
- do not log Authorization header, JWT, passwords, secrets or request bodies by default.

Do not implement a full request/response logging filter.

7. Logging pattern
Inspect current logging configuration.

If the current local log pattern does not expose MDC traceId, propose the smallest configuration change necessary.

Do not introduce JSON logging or a new logging framework in TECH-009.

8. Tests
Tests must remain hermetic and Docker-free.

Cover at least:
- request receives a traceId;
- traceId reaches ErrorResponse;
- response header contains the same traceId if response-header propagation is adopted;
- MDC is cleaned after request completion;
- separate requests do not share trace IDs;
- unexpected error uses the same request traceId.

If incoming IDs are accepted, test valid and invalid input behavior.

9. Non-HTTP contexts
Do not generalize correlation to Kafka, schedulers, jobs or async processing in this task.

10. Architecture
Keep Domain independent from:
- MDC;
- logging framework;
- servlet APIs;
- trace infrastructure.

Create a plan containing:

1. Current observability/logging state
2. Existing TECH-006 trace integration
3. Proposed trace lifecycle
4. Incoming-header decision
5. Response-header decision
6. ID-generation strategy
7. MDC/constants design
8. Logging configuration changes
9. Error-handler integration changes
10. Test strategy
11. Exact files expected to create/modify
12. Dependency changes
13. Validation commands
14. Scope compliance
15. Risks and assumptions

Explicitly confirm no:
- OpenTelemetry;
- Prometheus/Grafana;
- APM;
- distributed tracing;
- business metrics;
- request-body logging;
- Kafka correlation;
- authentication;
- authorization;
- business behavior.

Prefer the minimum correct implementation.

Do not implement yet.
```
