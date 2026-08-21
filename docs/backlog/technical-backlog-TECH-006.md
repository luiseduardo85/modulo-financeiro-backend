# Technical Backlog — TECH-006

## TECH-006 — Implementar contrato de erros da API

### Objetivo

Criar a infraestrutura comum de tratamento e serialização de erros da API REST.

A tarefa deve estabelecer um contrato consistente para respostas de erro sem implementar regras de negócio específicas de módulos financeiros.

### Prioridade

P0

### Dependências

- TECH-001 — Bootstrap Spring Boot
- TECH-002 — Estrutura Clean Architecture
- TECH-003 — PostgreSQL local
- TECH-004 — Flyway
- TECH-005 — PostgreSQL Testcontainers

### Documentação relacionada

- `AGENTS.md`
- `docs/api/errors.md`
- `docs/api/conventions.md`
- `docs/architecture/backend-architecture.md`
- `docs/backlog/technical-backlog.md`

### Escopo incluído

- criar modelo de resposta de erro;
- criar modelo de detalhe de validação;
- criar tratamento global de exceções para a camada REST;
- tratar erros de validação Bean Validation;
- tratar JSON/request malformado;
- criar mecanismo central para mapear exceções conhecidas em status HTTP;
- incluir `timestamp`;
- incluir `traceId` quando disponível no contexto;
- garantir formato estável de resposta;
- criar testes de API/infraestrutura para o contrato;
- documentar exemplos.

### Contrato esperado

Estrutura base:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Existem campos inválidos.",
  "details": [
    {
      "field": "campo",
      "code": "CAMPO_INVALIDO",
      "message": "Mensagem de validação."
    }
  ],
  "timestamp": "2026-08-21T10:00:00Z",
  "traceId": "..."
}
```

### Campos obrigatórios

- `code`
- `message`
- `details`
- `timestamp`
- `traceId`

`details` deve ser uma lista vazia quando não houver detalhes.

`traceId` pode ser nulo/ausente internamente caso o contexto ainda não forneça um valor, mas o Plan deve propor uma estratégia consistente.

### Categorias HTTP

A infraestrutura deve suportar o mapeamento conceitual:

- 400 — request malformado;
- 401 — não autenticado;
- 403 — não autorizado;
- 404 — recurso não encontrado;
- 409 — conflito de estado/concorrência;
- 422 — validação semântica/regra de entrada;
- 500 — erro interno.

A TECH-006 não deve criar regras concretas de autenticação nem erros específicos do domínio financeiro apenas para ocupar essas categorias.

### Exceções

Evitar `RuntimeException` genérica espalhada pelo projeto.

A tarefa pode introduzir uma pequena hierarquia técnica de exceções da aplicação/interfaces quando necessária para sustentar o contrato.

Não criar dezenas de subclasses sem uso concreto.

### Bean Validation

Erros de validação de request devem retornar `VALIDATION_ERROR`.

Cada detalhe deve conter:

- campo;
- código estável;
- mensagem.

O Plan deve avaliar como derivar o `code` de cada validação sem acoplar o contrato a mensagens humanas.

### Erro interno

Erros inesperados devem:

- retornar HTTP 500;
- usar código estável, como `INTERNAL_ERROR`;
- não expor stack trace;
- não expor detalhes internos/sensíveis;
- manter o erro técnico disponível nos logs.

### Logging

O tratamento global não deve:

- logar JWT;
- logar senha;
- logar secrets;
- expor stack trace no response.

A estratégia detalhada de observabilidade continua pertencendo ao TECH-009, portanto evitar expandir o escopo.

### Trace ID

O contrato prevê `traceId`.

A TECH-006 deve integrar-se ao mecanismo já existente, se houver.

Se ainda não houver traceId real, a implementação deve evitar criar uma solução de observabilidade complexa apenas para esta tarefa.

O Plan deve identificar a alternativa mínima.

### Arquitetura

Preferência de localização:

```text
interfaces/
  rest/
    error/
```

ou estrutura equivalente compatível com a arquitetura atual.

Não colocar tratamento HTTP no Domain.

### Fora do escopo

- erros específicos de ContaFinanceira;
- regras de aprovação;
- autenticação externa;
- autorização completa;
- observabilidade avançada;
- Kafka;
- internacionalização completa;
- catálogo definitivo de mensagens;
- persistence exception translation específica de repositories futuros.

### Restrições

Não:

- criar regras de negócio;
- expor exception message interna diretamente ao cliente;
- retornar stack trace;
- criar dependência do Domain para HTTP;
- implementar autenticação;
- criar catálogo gigantesco de exceptions especulativas;
- antecipar erros de UCs ainda não implementados.

### Testes mínimos esperados

Cobrir pelo menos:

1. request com Bean Validation inválida;
2. JSON/request malformado;
3. exceção de recurso não encontrado usando uma exceção técnica de teste/infra;
4. exceção inesperada -> 500;
5. contrato JSON consistente;
6. ausência de stack trace/detalhes internos no response.

### Critérios de aceite

- [ ] ErrorResponse definido;
- [ ] ValidationErrorDetail definido;
- [ ] GlobalExceptionHandler definido;
- [ ] Bean Validation mapeada;
- [ ] JSON malformado mapeado;
- [ ] 404 suportado por exceção apropriada;
- [ ] 409/422 possuem mecanismo de mapeamento preparado sem regras financeiras inventadas;
- [ ] 500 não expõe detalhes internos;
- [ ] timestamp usa formato ISO-8601;
- [ ] traceId possui estratégia mínima e documentada;
- [ ] testes do contrato passam;
- [ ] Domain não depende de HTTP/Spring MVC;
- [ ] nenhuma autenticação foi implementada;
- [ ] `mvn test` passa;
- [ ] `mvn verify` continua passando quando Docker estiver disponível;
- [ ] `mvn package` passa.
