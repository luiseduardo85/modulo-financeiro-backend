# Migration Guidelines

Flyway é oficial.

1. Nunca editar migration já aplicada.
2. Criar nova migration para mudanças.
3. Preferir mudanças backward-safe.
4. Identificadores usam camelCase.
5. Quote identificadores PostgreSQL quando necessário.
6. Não usar Hibernate ddl-auto=update.
7. Mudanças destrutivas exigem revisão explícita.
8. Dados financeiros não podem ser descartados silenciosamente.
9. Testes executam migrations em PostgreSQL Testcontainer.

Local: `backend/src/main/resources/db/migration/`.
