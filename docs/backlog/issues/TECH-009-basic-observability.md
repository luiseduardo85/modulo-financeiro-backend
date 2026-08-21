# TECH-009 — Implementar observabilidade básica

## Objetivo

Adicionar correlação mínima de requisições HTTP e convenções básicas de logging.

## Dependências

- TECH-001
- TECH-002
- TECH-006
- TECH-008

## Incluído

- traceId por request;
- MDC;
- integração com ErrorResponse;
- limpeza do contexto;
- header de correlação, se aprovado;
- política básica de logs;
- testes herméticos.

## Fora do escopo

- OpenTelemetry;
- Prometheus;
- Grafana;
- APM;
- tracing distribuído;
- métricas de negócio;
- body logging;
- Kafka correlation;
- autenticação/autorização.

## Critérios de aceite

- [ ] traceId existe em requests HTTP
- [ ] MDC recebe traceId
- [ ] MDC é limpo
- [ ] ErrorResponse usa traceId real
- [ ] política de logs definida
- [ ] testes passam sem Docker
- [ ] nenhum dado sensível é logado
