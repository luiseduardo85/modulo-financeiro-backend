# FUNC-003 — Category / Cost Center

## Objetivo
Implementar Category e CostCenter por Company, compartilhados entre futuras contas a pagar e receber.

Também introduzir Spotless como formatter Java global e reprodutível do projeto.

## Regras confirmadas
- Category pertence a exatamente uma Company.
- CostCenter pertence a exatamente uma Company.
- Ambos são compartilhados entre PAYABLE e RECEIVABLE.
- Não adicionar type PAYABLE/RECEIVABLE.
- Inativos não podem ser usados em novos lançamentos futuros.
- Histórico permanece válido.
- Sem exclusão física por inativação.
- companyId de rota é apenas resource scope até autenticação futura.
- Código/API/banco em inglês; mensagens ao usuário podem ser em português.

## Escopo
- Domain, Application, JPA Infrastructure, REST, Flyway, paginação, erros e testes.
- lifecycle mínimo active/inactive.
- Spotless Maven para Java.

## Fora do escopo
- hierarquia de Category/CostCenter;
- códigos contábeis;
- rateio/budget;
- BankAccount/PaymentMethod/FinancialAccount;
- auth;
- Kafka/Redis/outbox.
