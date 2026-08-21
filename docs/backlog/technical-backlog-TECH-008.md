# Technical Backlog — TECH-008

## TECH-008 — Definir convenções de testes

### Objetivo

Estabelecer as convenções oficiais de testes do backend antes da implementação dos primeiros módulos de negócio.

A TECH deve definir claramente:

- tipos de teste;
- responsabilidades de cada nível;
- nomenclatura;
- isolamento;
- uso de mocks;
- uso de Spring;
- uso de PostgreSQL Testcontainers;
- execução via Maven;
- organização dos testes no projeto.

A tarefa não deve criar uma infraestrutura genérica ou abstrata além do necessário.

### Prioridade

P0

### Dependências

- TECH-001 — Bootstrap Spring Boot
- TECH-002 — Estrutura Clean Architecture
- TECH-003 — PostgreSQL local
- TECH-004 — Flyway
- TECH-005 — PostgreSQL Testcontainers
- TECH-006 — Contrato de erros da API
- TECH-007 — Convenções de persistência

### Documentação relacionada

- `AGENTS.md`
- `docs/architecture/testing.md`
- `docs/architecture/backend-architecture.md`
- `docs/architecture/persistence.md`
- `docs/architecture/transactions.md`
- `docs/development/definition-of-done.md`
- `docs/backlog/technical-backlog.md`

### Pirâmide de testes

O projeto deve seguir uma pirâmide pragmática:

```text
            E2E
             ▲
           poucos
        API / REST
           Integration
        Application
          Domain
        muitos e rápidos
```

Ordem de preferência:

1. Domain unit tests
2. Application/use-case tests
3. Persistence integration tests
4. REST/API tests
5. poucos E2E

### Testes de Domain

Características:

- JUnit;
- sem Spring;
- sem banco;
- sem Docker;
- sem mocks quando objetos simples/fakes forem suficientes;
- rápidos e determinísticos.

Devem testar:

- invariantes;
- transições de estado;
- regras de negócio;
- cálculos;
- Value Objects;
- comportamentos de aggregates.

Não devem testar:

- getters/setters triviais;
- annotations;
- detalhes de framework.

### Testes de Application

Características:

- preferencialmente sem Spring;
- use case instanciado diretamente;
- ports substituídos por fakes/stubs/mocks conforme necessário;
- sem PostgreSQL;
- sem Docker.

Devem testar:

- orchestration;
- autorização interna quando aplicável;
- chamadas aos ports;
- transações conceituais;
- regras de fluxo entre aggregates/services.

Evitar mocks excessivamente acoplados à implementação.

### Persistence integration tests

Características:

- PostgreSQL 16;
- Testcontainers;
- Flyway migrations reais;
- JPA/Hibernate reais;
- sem H2;
- sem `.env`;
- sem PostgreSQL local.

Devem testar:

- mappings JPA;
- constraints;
- queries;
- repository adapters;
- optimistic locking;
- comportamento real do PostgreSQL quando relevante.

Nomenclatura:

`*IT`

Execução:

`mvn verify`

### REST/API tests

Devem testar:

- status HTTP;
- JSON de request/response;
- validação;
- contrato de erros;
- serialização;
- mapeamento adapter/application.

Preferir testes focados sem banco quando o banco não for necessário.

Não transformar todo teste de controller em `@SpringBootTest`.

O Plan deve analisar qual estratégia do Spring Boot atual é mais adequada para testes REST isolados.

### E2E

Fora do escopo desta TECH implementar E2E.

Convenção:

- poucos;
- reservados para fluxos críticos;
- adicionados apenas quando houver aplicação funcional suficiente.

### Maven

Convenção oficial:

```text
mvn test
```

Executa testes herméticos e não deve exigir Docker.

```text
mvn verify
```

Executa também integration tests `*IT` via Failsafe e pode exigir Docker.

A execução de `mvn test` deve permanecer apropriada para feedback rápido de desenvolvimento.

### Naming de classes

Convenções:

- unit/application/API herméticos: `*Test`
- integração com infraestrutura real: `*IT`

Exemplos futuros:

- `ContaFinanceiraTest`
- `CriarContaFinanceiraUseCaseTest`
- `ContaFinanceiraControllerTest`
- `ContaFinanceiraRepositoryIT`

Não criar categorias adicionais sem necessidade.

### Estrutura

Os testes devem refletir o contexto/pacote do código testado.

Exemplo futuro:

```text
src/test/java/com/financeiro/conta/
  domain/
    ContaFinanceiraTest.java
  application/
    CriarContaFinanceiraUseCaseTest.java
  infrastructure/
    persistence/
      ContaFinanceiraRepositoryIT.java
  interfaces/
    rest/
      ContaFinanceiraControllerTest.java
```

Não centralizar todos os testes em pacote `tests` genérico.

### Mocks

Mocks são ferramenta, não padrão obrigatório.

Preferência:

- Domain: normalmente sem mocks;
- Application: mock/fake apenas para ports externos;
- Integration: não mockar JPA/PostgreSQL/Flyway;
- REST: mockar a fronteira Application quando o objetivo for testar somente HTTP.

Evitar:

- mocking de objetos de domínio simples;
- mocks de detalhes internos;
- testes que apenas reproduzem implementação;
- `verify(...)` excessivo sem valor comportamental.

### Dados de teste

Preferir builders/factories locais e explícitos quando o domínio começar a crescer.

Não criar agora:

- `TestDataFactory` global;
- `Mother` genérica para todo sistema;
- framework próprio de fixtures;
- builders para entidades que ainda não existem.

Fixtures devem permanecer próximas do contexto que utilizam.

### Clock e tempo

Código de negócio dependente de "agora" deve receber fonte de tempo testável quando necessário.

Preferir `Clock` ou abstração equivalente somente quando houver necessidade real.

Não usar sleeps para testar tempo.

### Banco e limpeza

Integration tests devem usar banco isolado do Testcontainers.

A estratégia de limpeza entre testes deve ser a mais simples possível e compatível com o caso:

- rollback transacional quando apropriado;
- criação explícita/limpeza de dados;
- novo contexto/container somente quando necessário.

Não implementar framework de reset de banco nesta TECH.

### Assertions

Preferir assertions claras e comportamentais.

Não adicionar biblioteca extra de assertions apenas por preferência estética.

Usar o stack já disponível, salvo necessidade comprovada.

### Cobertura

Não definir percentual mínimo global de cobertura nesta TECH.

Cobertura pode ser medida futuramente, mas não deve virar meta artificial.

Critério principal:

- regras críticas cobertas;
- comportamento relevante validado;
- regressões importantes protegidas.

### Testes e arquitetura

Testes devem respeitar as mesmas fronteiras arquiteturais do código.

Não usar testes para justificar dependências proibidas entre camadas.

### Fora do escopo

- cobertura mínima percentual;
- JaCoCo obrigatório;
- mutation testing;
- E2E;
- performance/load tests;
- contract testing externo;
- testes de autenticação externa;
- pipeline CI/CD completo;
- mocks/fakes genéricos do sistema;
- fixtures globais;
- novas regras de negócio.

### Restrições

Não:

- adicionar H2;
- tornar `mvn test` dependente de Docker;
- usar Spring em testes de Domain;
- criar `AbstractIntegrationTest` genérico sem necessidade;
- criar framework próprio de testes;
- criar dados de negócio artificiais;
- antecipar entidades/Use Cases;
- testar implementação trivial em vez de comportamento.

### Critérios de aceite

- [ ] tipos de testes documentados;
- [ ] convenção `*Test` / `*IT` documentada;
- [ ] `mvn test` continua hermético;
- [ ] `mvn verify` continua responsável pelos integration tests;
- [ ] PostgreSQL 16 + Testcontainers permanece padrão de integração;
- [ ] H2 continua proibido;
- [ ] convenção de mocks definida;
- [ ] convenção de fixtures definida;
- [ ] organização dos pacotes de teste definida;
- [ ] testes de Domain sem Spring documentados;
- [ ] testes Application preferencialmente sem Spring documentados;
- [ ] REST tests focados documentados;
- [ ] persistence IT documentados;
- [ ] nenhum framework de testes especulativo criado;
- [ ] nenhuma regra de negócio antecipada;
- [ ] testes existentes continuam passando;
- [ ] `mvn test` passa;
- [ ] `mvn package` passa;
- [ ] `mvn verify` passa quando Docker estiver disponível.
