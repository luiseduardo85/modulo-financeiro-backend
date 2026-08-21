# Testing Strategy

- Domain Unit Tests: sem Spring.
- Application Tests: Use Cases com mocks/fakes.
- Repository Integration Tests: PostgreSQL Testcontainers + Flyway.
- Não usar H2 como substituto.
- API Tests: contrato, status, validação e autorização.
- E2E: poucos fluxos críticos.

Cobertura é métrica auxiliar; regras críticas precisam de testes comportamentais completos.
