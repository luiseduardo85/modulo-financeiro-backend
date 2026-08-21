# Codex Plan — TECH-006

```text
Read AGENTS.md first.

We are planning:

TECH-006 — Implement API error contract.

Before proposing changes, read:

- docs/api/errors.md
- docs/api/conventions.md
- docs/architecture/backend-architecture.md
- docs/backlog/technical-backlog.md
- docs/backlog/issues/TECH-006-api-error-contract.md

Inspect the current backend after TECH-005.

Do not modify files yet.

Goal:
Create the common REST error-handling infrastructure and a stable error response contract.

Expected base contract:

{
  "code": "...",
  "message": "...",
  "details": [],
  "timestamp": "...",
  "traceId": "..."
}

Scope:
- ErrorResponse;
- ValidationErrorDetail;
- global REST exception handling;
- Bean Validation errors;
- malformed JSON/request errors;
- technical support for 404, 409 and 422 mappings;
- safe generic 500 handling;
- timestamp;
- minimal traceId integration;
- focused API/contract tests.

HTTP categories:
- 400 malformed request;
- 401 unauthenticated;
- 403 unauthorized;
- 404 resource not found;
- 409 conflict/concurrency/state conflict;
- 422 semantic/input validation;
- 500 internal error.

Important:
TECH-006 must create infrastructure, not financial business errors.

Do not create ContaFinanceira-specific exceptions or error codes.

Explicitly out of scope:
- financial business rules;
- approval errors;
- external authentication;
- full authorization implementation;
- advanced observability;
- Kafka;
- internationalization framework;
- repository/persistence exception translation for repositories that do not exist yet.

Architecture:
- HTTP-specific error handling belongs to the interfaces/REST side;
- Domain must not depend on HTTP or Spring MVC;
- avoid a large speculative exception hierarchy.

Bean Validation:
Analyze how field-level error codes can remain stable without coupling the API contract to human-readable validation messages.

500 behavior:
- never return stack traces;
- never return raw internal exception messages;
- keep technical diagnostics in logs only.

traceId:
Inspect whether the current project already has a trace/correlation identifier.
If not, propose the smallest compatible strategy.
Do not implement a complete observability framework in TECH-006.

Create a plan containing:

1. Current-state analysis
   - current REST dependencies;
   - validation setup;
   - existing exception handling;
   - existing logging/trace support.

2. Proposed package/file structure
   - exact paths;
   - why each component belongs there.

3. Error model
   - ErrorResponse fields/types;
   - ValidationErrorDetail fields/types;
   - timestamp representation;
   - details empty-list behavior;
   - traceId behavior.

4. Exception strategy
   - minimal exception types actually required now;
   - mapping to HTTP statuses;
   - how future domain/application exceptions can integrate without changing the response contract.

5. Bean Validation mapping
   - field extraction;
   - stable error code strategy;
   - message strategy.

6. Malformed request handling

7. Unexpected error handling
   - response;
   - logging;
   - data leakage prevention.

8. Tests
   - exact scenarios;
   - whether MockMvc/current Spring Boot test support is appropriate;
   - keep tests hermetic and Docker-free when possible.

9. Files
   - exact files expected to create;
   - exact files expected to modify.

10. Validation commands
   - mvn test;
   - mvn package;
   - mvn verify if relevant and Docker is available.

11. Scope compliance
   Confirm explicitly that the plan adds:
   - no financial business behavior;
   - no authentication implementation;
   - no authorization implementation;
   - no Kafka;
   - no persistence/domain schema changes.

12. Risks and assumptions
   - Spring Boot 4.1.1 exception APIs;
   - traceId availability;
   - validation code derivation;
   - documentation conflicts.

Prefer the minimum reusable error infrastructure necessary for future Use Cases.

Do not implement yet.
```
