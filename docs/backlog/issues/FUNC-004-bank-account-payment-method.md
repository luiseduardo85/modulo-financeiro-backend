# FUNC-004 — Bank Account / Payment Method

## Objetivo

Implementar os cadastros de BankAccount e PaymentMethod necessários antes do
FinancialAccount.

## Regras confirmadas

### BankAccount
- pertence obrigatoriamente a exatamente uma Company;
- não é compartilhada entre Companies;
- `branchId` nulo permite uso por todas as Branches da Company;
- `branchId` informado restringe a uma unica Branch, que deve pertencer a mesma Company;
- inativa não pode ser utilizada em novas movimentações futuras;
- histórico existente deve permanecer válido;
- não deve ser fisicamente excluída apenas por inativação.

### PaymentMethod
- pertence obrigatoriamente a exatamente uma Company;
- pode ser utilizada tanto em pagamentos quanto em recebimentos;
- não deve possuir tipo exclusivo PAYABLE/RECEIVABLE;
- lifecycle active/inactive deve ser modelado somente conforme regras documentadas;
- histórico existente deve permanecer válido.

Ambos possuem apenas ID, ownership escalar, nome e active (alem do `branchId`
opcional de BankAccount). Nomes sao normalizados com `String.strip()`, limitados
a 200 caracteres e podem se repetir. Criacao e ativa e somente desativacao e
suportada; nao ha reativacao ou exclusao.

### Convenções
- código, REST, JSON, enums e banco em inglês;
- mensagens ao usuário podem ficar em português;
- Domain sem Spring/JPA;
- JPA Infrastructure separada;
- PostgreSQL 16 + Flyway;
- Java deve ser Spotless-compliant;
- sem abstrações CRUD genéricas.

## Escopo

- BankAccount Domain/Application/Infrastructure/REST;
- PaymentMethod Domain/Application/Infrastructure/REST;
- migrations;
- paginação/sort;
- lifecycle mínimo;
- erros estáveis;
- testes herméticos;
- PostgreSQL/Testcontainers;
- documentação canônica.

## Fora do escopo

- FinancialAccount;
- movimentações;
- saldo bancário;
- conciliação;
- integração bancária/Open Finance;
- PIX API;
- boleto;
- cartão/gateway;
- autenticação/autorização;
- Kafka/Redis/outbox;
- rateio;
- regras financeiras de liquidação.
