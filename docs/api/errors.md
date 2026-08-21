# API Error Contract

Campos: code, message, details, timestamp, traceId.

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
