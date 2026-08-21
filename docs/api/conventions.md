# API Conventions

Base: `/api/v1`.
Recursos no plural.
Ações de domínio usam POST explícito, como `/contas/{id}/aprovar`.

Status não pode ser alterado diretamente por payload.

Operações normais não confiam em empresaId enviado pelo cliente.

Java, rotas REST, campos JSON, códigos técnicos e identificadores de banco usam
nomes em inglês. Para o slice Company / Branch, os nomes canônicos são
`Company`, `Branch`, `company`, `branch` e `companyId`. Mensagens apresentadas ao
usuário podem permanecer em português.

Nos endpoints aninhados de Branch, o `companyId` da rota é apenas escopo do
recurso enquanto autenticação e autorização estiverem diferidas. Ele ainda não é
um contexto de tenant autenticado e confiável. O corpo de criação de Branch não
aceita `companyId`.

Business dates: YYYY-MM-DD.
Timestamps: ISO-8601, preferencialmente UTC.
Money: JSON decimal -> BigDecimal.

Operacoes financeiras futuras que declarem suporte a idempotencia exigem o header
`Idempotency-Key`. A chave e gerada pelo cliente, opaca, case-sensitive e deve
conter de 1 a 128 caracteres ASCII visiveis (`!` a `~`). O valor nao e normalizado
nem substituido por uma chave gerada pelo servidor.
