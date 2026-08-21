# Technical Backlog — TECH-009

## TECH-009 — Implementar observabilidade básica

### Objetivo

Estabelecer a infraestrutura mínima de observabilidade do backend para permitir rastreamento de requisições e logs consistentes antes da implementação dos principais casos de uso de negócio.

A TECH deve introduzir apenas o necessário para:

- gerar/propagar um traceId por requisição HTTP;
- disponibilizar esse traceId via MDC;
- permitir que o contrato de erro da API utilize o traceId real;
- padronizar o logging técnico básico;
- evitar vazamento de dados sensíveis.

Não deve introduzir uma plataforma completa de observabilidade.

### Prioridade

P0

### Dependências

- TECH-001 — Bootstrap Spring Boot
- TECH-002 — Estrutura Clean Architecture
- TECH-006 — Contrato de erros da API
- TECH-008 — Convenções de testes

### Documentação relacionada

- `AGENTS.md`
- `docs/architecture/backend-architecture.md`
- `docs/api/errors.md`
- `docs/architecture/testing.md`
- `docs/backlog/technical-backlog.md`

### Escopo incluído

- criar filtro/interceptor HTTP para trace/correlation ID;
- gerar traceId quando não houver um ID interno válido;
- colocar traceId no MDC durante o processamento da request;
- limpar MDC ao final da request;
- integrar o traceId com `TraceIdProvider`;
- opcionalmente devolver o traceId em header de resposta;
- definir formato mínimo de logs;
- documentar política de logging;
- criar testes herméticos para correlação;
- garantir que erros 500 continuem seguros.

### Trace ID

O traceId deve:

- existir para toda requisição HTTP processada;
- ser colocado no MDC;
- estar disponível ao GlobalExceptionHandler;
- ser removido do MDC ao final da request;
- não depender de autenticação;
- não depender de banco;
- não depender de Kafka.

O Plan deve decidir se um header externo será aceito como entrada.

Por padrão, não confiar cegamente em um header arbitrário fornecido pelo cliente.

Se houver suporte a propagação de header, ele deve ser explicitamente validado e documentado.

### Header de resposta

Preferência:

`X-Trace-Id`

O backend pode retornar o traceId gerado/aceito no response header para facilitar suporte e correlação.

O nome final deve ser documentado e centralizado em constante.

### Geração do ID

Preferir uma estratégia simples e local.

Exemplo aceitável:

- UUID aleatório em formato string.

Não adicionar biblioteca externa apenas para gerar IDs.

### MDC

Usar SLF4J MDC.

Chave oficial:

`traceId`

A chave deve ser centralizada e reutilizada por:

- filtro;
- TraceIdProvider;
- logging;
- testes.

Não duplicar a string literal em vários pontos.

### Logging

A TECH deve estabelecer convenções, não um framework novo.

Logs técnicos devem:

- incluir traceId quando houver request;
- usar níveis adequados;
- preservar stack trace server-side para falhas inesperadas;
- não expor stack trace ao cliente.

Não logar:

- senha;
- token/JWT;
- Authorization header;
- cookies de sessão;
- secrets;
- request body completo por padrão;
- dados financeiros sensíveis sem necessidade explícita.

### Formato

O Plan deve avaliar se a configuração atual de logging já inclui MDC no pattern.

Se não incluir, propor a menor alteração possível para logs locais.

Exemplo conceitual:

`traceId=<valor>`

Não introduzir JSON logging estruturado nesta TECH, salvo se já estiver presente.

### Request logging

Não implementar logging completo de request/response nesta etapa.

Não logar automaticamente:

- body;
- todos os headers;
- payloads financeiros.

O filtro de correlação deve ser focado em contexto, não em auditoria HTTP.

### Error contract

O `traceId` do `ErrorResponse` deve deixar de ser normalmente nulo durante requisições HTTP.

O GlobalExceptionHandler deve continuar:

- retornando mensagem segura;
- não expondo exception message interna;
- logando detalhes técnicos server-side.

### Contextos não HTTP

Esta TECH cobre requisições HTTP.

Não criar solução genérica para:

- Kafka;
- jobs;
- scheduler;
- async workers.

Quando esses mecanismos forem implementados, deverão definir sua própria propagação de contexto.

### Testes mínimos

Cobrir:

1. request sem trace header recebe traceId;
2. traceId fica disponível no ErrorResponse;
3. response contém `X-Trace-Id`, se essa decisão for adotada;
4. MDC é limpo após a request;
5. duas requests distintas não compartilham traceId;
6. erro 500 mantém o mesmo traceId da request;
7. testes continuam herméticos, sem Docker.

Se entrada de trace header for suportada:

8. ID válido pode ser propagado;
9. ID inválido não é aceito cegamente.

### Arquitetura

A infraestrutura de correlação pertence à camada Interfaces/Infrastructure técnica.

Não adicionar dependência ao Domain.

Possível estrutura:

```text
interfaces/
  rest/
    trace/
      TraceIdFilter.java
      TraceContext.java
```

ou estrutura equivalente compatível com o projeto atual.

Evitar criar abstração genérica de observabilidade sem uso concreto.

### Fora do escopo

- OpenTelemetry;
- Micrometer avançado;
- Prometheus;
- Grafana;
- APM;
- tracing distribuído;
- spans;
- métricas de negócio;
- request/response body logging;
- auditoria financeira;
- Kafka correlation;
- autenticação;
- autorização;
- dashboards operacionais;
- alertas.

### Restrições

Não:

- adicionar dependência pesada de observabilidade;
- criar tracing distribuído;
- confiar cegamente em traceId vindo do cliente;
- logar secrets/tokens;
- logar request body completo;
- alterar regras de negócio;
- acoplar Domain a logging/MDC;
- tornar testes dependentes de Docker.

### Critérios de aceite

- [ ] traceId gerado para requests HTTP;
- [ ] traceId disponível no MDC;
- [ ] chave MDC centralizada;
- [ ] TraceIdProvider integrado;
- [ ] ErrorResponse usa traceId real;
- [ ] MDC limpo ao final da request;
- [ ] política de logging documentada;
- [ ] dados sensíveis não são logados;
- [ ] testes de correlação passam;
- [ ] testes continuam herméticos;
- [ ] nenhuma dependência pesada de observabilidade adicionada;
- [ ] nenhuma autenticação/autorização implementada;
- [ ] `mvn test` passa;
- [ ] `mvn package` passa;
- [ ] `mvn verify` continua passando quando Docker estiver disponível.
