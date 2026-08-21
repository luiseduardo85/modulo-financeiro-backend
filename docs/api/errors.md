# API Error Contract

Campos: code, message, details, timestamp, traceId.

Contrato base:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "There are invalid fields.",
  "details": [
    {
      "field": "name",
      "code": "NOT_BLANK",
      "message": "must not be blank"
    }
  ],
  "timestamp": "2026-08-21T10:00:00Z",
  "traceId": null
}
```

`details` deve ser uma lista vazia quando o erro nao possuir detalhes de campos.
`timestamp` usa ISO-8601 em UTC. Durante uma requisicao HTTP, `traceId` contem o
UUID gerado pelo servidor para a requisicao e possui o mesmo valor retornado no
header de resposta `X-Trace-Id`. Identificadores enviados pelo cliente nao sao
aceitos nem propagados. Fora de um contexto HTTP, `traceId` pode ser nulo.

Codigos tecnicos iniciais:

- `MALFORMED_REQUEST`: corpo da requisicao ilegivel ou JSON malformado;
- `VALIDATION_ERROR`: Bean Validation ou validacao semantica de entrada;
- `RESOURCE_NOT_FOUND`: recurso nao encontrado;
- `CONFLICT`: conflito de estado ou concorrencia;
- `INTERNAL_ERROR`: falha interna inesperada.

HTTP:
200 consulta/ação com resposta
201 criação
204 sem body
400 request malformado
401 não autenticado
403 não autorizado
404 não encontrado
409 conflito de estado/concorrência
422 validação semântica
500 erro interno
