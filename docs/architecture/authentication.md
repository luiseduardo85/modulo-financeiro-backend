# Authentication

## Status
PENDING EXTERNAL AUTH SERVICE DEFINITION

Ainda não definidos: protocolo, token, claims, validação, refresh e vínculo da identidade externa ao Usuario interno.

Não implementar autenticação até esta documentação ser completada.

## Limite temporário do workflow de aprovação

`ApprovalActorContext` é a porta confiável da Application para obter o
`ApprovalActor` atual. `ApprovalEligibility` é a porta de autorização de
negócio que verifica `CONTA_APROVAR` no contexto da Company. Até existir o
adaptador do serviço externo, a infraestrutura falha fechada: não interpreta
payload, path variable ou header público arbitrário como identidade confiável.

JWT, OIDC, login e o formato da identidade externa continuam deferidos.

Enquanto o contexto confiavel nao estiver disponivel, as acoes respondem com
HTTP 401 e o codigo estavel `APPROVAL_ACTOR_REQUIRED`. Esse contrato de
transporte preserva o comportamento fail-closed e nao implementa autenticacao.
