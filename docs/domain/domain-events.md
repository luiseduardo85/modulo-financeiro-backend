# Domain Events

Eventos candidatos:
ContaCriada, ContaEnviadaParaAprovacao, ContaAprovada, ContaRejeitada, ContaCancelada, ContaQuitada, MovimentacaoCriada, MovimentacaoEstornada.

Domain Event não depende de Kafka. Integrações assíncronas externas podem usar Outbox.
