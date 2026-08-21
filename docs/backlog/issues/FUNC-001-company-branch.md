# Functional Slice — Company / Branch

## Objetivo
Implementar o primeiro slice funcional do domínio: Company e Branch.

## Regras confirmadas
- O sistema é multi-company.
- Branch pertence obrigatoriamente a exatamente uma Company.
- Branch não pode ser compartilhada entre Companies.
- Future FinancialAccount exigirá Branch.
- Usuário por filial não faz parte deste slice.
- Autenticação/autorização permanecem fora do escopo.
- Código, API e banco em inglês; mensagens para usuário podem ficar em português.

## Escopo
- Domain entities Company e Branch.
- invariantes mínimas.
- repository ports.
- application use cases.
- JPA Infrastructure separada do Domain.
- Flyway.
- REST `/api/v1`.
- testes unitários, MVC e PostgreSQL/Testcontainers.

## Fora do escopo
User, Profile, Permission, auth, Partner, Category, CostCenter, BankAccount,
PaymentMethod, FinancialAccount, Approval, Settlement, Reversal, Kafka, Redis e outbox.

## Regra de modelagem
Não inventar campos comuns de ERP (documento fiscal, endereço, contatos,
inscrição estadual etc.) sem suporte explícito da documentação.

## Decisões aprovadas

- nomes canônicos em Java, REST, JSON e banco: `Company`, `Branch`, `company`,
  `branch` e `companyId`;
- Company possui somente `id: Long` e `name: String`;
- Branch possui somente `id: Long`, `companyId: Long` e `name: String`;
- nomes são obrigatórios, normalizados pela remoção de espaços externos, não
  podem ficar em branco, possuem no máximo 200 caracteres e não são únicos;
- Branch referencia Company por ID imutável, sem navegação JPA entre aggregates;
- paginação usa `page=0`, `size=20`, máximo 100 e `sort=id,asc` por padrão;
- sort aceita somente `id|name` e `asc|desc`;
- `companyId` em rota é escopo de recurso, não contexto autenticado de tenant;
- CRUD de Company/Branch não usa a idempotência financeira do TECH-010.
