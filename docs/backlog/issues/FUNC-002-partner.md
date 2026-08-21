# FUNC-002 — Partner

## Objetivo
Implementar o cadastro global de Partner, que poderá atuar como CUSTOMER,
SUPPLIER ou ambos, e será referenciado futuramente por FinancialAccount.

## Regras confirmadas
- Partner é global e não pertence a Company.
- O mesmo Partner pode atuar como CUSTOMER, SUPPLIER ou ambos.
- Documento é CPF ou CNPJ.
- Documento deve ser globalmente único e validado.
- Partner fica disponível imediatamente após o cadastro.
- Partner inativo não pode ser usado em novos lançamentos futuros.
- Histórico existente permanece preservado.
- Não criar PartnerCompany / ParceiroEmpresa.
- Código, API, enums e banco usam inglês.
- Mensagens destinadas ao usuário podem ficar em português.

## Escopo
- Partner domain entity.
- Document value object.
- Partner roles.
- active/inactive somente no nível necessário para a regra confirmada.
- repository port.
- create/get/list use cases.
- JPA Infrastructure separada do Domain.
- Flyway.
- REST `/api/v1/partners`.
- erros estáveis.
- testes herméticos e PostgreSQL/Testcontainers.

## Fora do escopo
- Company/Branch changes.
- Partner por Company.
- FinancialAccount.
- contas a pagar/receber.
- approval.
- settlement/reversal.
- authentication/authorization.
- integração fiscal externa real.
- endereço/telefone/email.
- inscrição estadual/municipal.
- Kafka/outbox.
- importação de parceiros.

## Regra de modelagem
Não adicionar automaticamente dados fiscais/comerciais comuns de ERP.
A entidade deve começar com o menor conjunto necessário para ser utilizável
pelos futuros lançamentos financeiros.
