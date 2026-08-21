# Logging

## HTTP request correlation

Every HTTP request receives a server-generated UUID trace ID. The value is
available under the SLF4J MDC key `traceId` while the request is processed and
is returned in the `X-Trace-Id` response header. Client-provided trace IDs are
not accepted or propagated.

The request filter removes only the MDC value it owns when processing finishes,
including exceptional completion. Correlation for Kafka, asynchronous work,
scheduled jobs, and other non-HTTP contexts is outside the current scope.

## Safe logging conventions

- Unexpected HTTP failures are logged at error level with their throwable and
  stack trace server-side. The client continues to receive a safe error message.
- Request-scoped logs include the trace ID through MDC when it is available.
- Do not log Authorization headers, JWTs or other tokens, passwords, secrets,
  session cookies, complete request bodies, or financial payloads by default.
- Do not add general request/response logging without a separate documented need.

Local logs use Spring Boot's correlation pattern to render the request trace ID.
No JSON logging or additional logging framework is required.

## Scope decision

The broad technical backlog mentions Actuator and health under TECH-009. The
specific approved TECH-009 issue covers only basic HTTP correlation and safe
logging, so Actuator and health endpoints are not implemented by this task.
