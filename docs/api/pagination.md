# Pagination

Parâmetros: page, size, sort.
Defaults: page=0, size=20, max size=100.
Campos de sort devem estar em whitelist.

Resposta inclui `data` e `meta` com page, size, totalElements e totalPages.

Para Company e Branch:

- `page`: padrão 0, mínimo 0;
- `size`: padrão 20, mínimo 1, máximo 100;
- `sort`: exatamente `id,asc`, `id,desc`, `name,asc` ou `name,desc`;
- ordenação padrão: `id,asc`.

Parâmetros inválidos retornam HTTP 422 com `VALIDATION_ERROR`. Os campos de
ordenação são traduzidos por whitelist; valores arbitrários não são repassados
à persistência.

Partner usa a mesma paginação e aceita somente `id` e `name` com `asc`/`desc`,
usando `id,asc` por padrão. Não há filtros por documento, role ou active.
