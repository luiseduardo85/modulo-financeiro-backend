# API Conventions

Base: `/api/v1`.
Recursos no plural.
Ações de domínio usam POST explícito, como `/contas/{id}/aprovar`.

Status não pode ser alterado diretamente por payload.

Operações normais não confiam em empresaId enviado pelo cliente.

Business dates: YYYY-MM-DD.
Timestamps: ISO-8601, preferencialmente UTC.
Money: JSON decimal -> BigDecimal.
