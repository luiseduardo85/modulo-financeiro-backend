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
- `COMPANY_NOT_FOUND`: Company não encontrada;
- `BRANCH_NOT_FOUND`: Branch não encontrada no escopo da Company informada;
- `BANK_ACCOUNT_NOT_FOUND`: BankAccount nao encontrada no escopo da Company informada;
- `PAYMENT_METHOD_NOT_FOUND`: PaymentMethod nao encontrado no escopo da Company informada;
- `CONFLICT`: conflito de estado ou concorrencia;
- `INTERNAL_ERROR`: falha interna inesperada.
- `IDEMPOTENCY_KEY_REQUIRED`: header `Idempotency-Key` obrigatorio ausente;
- `INVALID_IDEMPOTENCY_KEY`: header `Idempotency-Key` invalido;
- `IDEMPOTENCY_KEY_CONFLICT`: chave reutilizada para comando materialmente diferente;
- `IDEMPOTENCY_REQUEST_IN_PROGRESS`: registro tecnico ainda em processamento.

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
