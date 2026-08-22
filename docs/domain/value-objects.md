# Value Objects

## Dinheiro
Java: BigDecimal.
PostgreSQL: NUMERIC(19,2).

## Documento
CPF/CNPJ normalizado e validado.

O nome técnico canônico é `Document`. CPF aceita 11 dígitos, com ou sem a
formatação convencional. CNPJ aceita o formato legado numérico e o formato
alfanumérico oficial com 12 posições alfanuméricas e dois dígitos verificadores,
com ou sem a formatação convencional. Espaços externos são removidos e letras
minúsculas de CNPJ são normalizadas para maiúsculas. A representação canônica
não contém pontuação; CPF permanece numérico e CNPJ pode conter letras nas 12
primeiras posições. Formato arbitrário, dígitos repetidos e dígitos verificadores
inválidos são rejeitados. `DocumentType` (`CPF`/`CNPJ`) é derivado do comprimento
e não persistido.

## Justificativa
Não nula, não vazia e sujeita a limite de tamanho documentado.
