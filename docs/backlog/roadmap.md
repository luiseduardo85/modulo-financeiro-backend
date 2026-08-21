# Roadmap de Implementação v1.0

## Objetivo

Definir a ordem oficial de implementação do projeto, respeitando dependências técnicas e funcionais.

A regra geral é:

```text
Foundation
  ↓
Empresa / Filial
  ↓
Cadastros auxiliares
  ↓
ContaFinanceira
  ↓
Aprovação
  ↓
Liquidação
  ↓
Estorno
  ↓
Histórico
  ↓
Fluxo de Caixa
  ↓
Dashboard
  ↓
Relatórios
  ↓
Notificações
  ↓
Autorização completa
  ↓
Integração com autenticação externa
```

## Fase 0 — Foundation

- TECH-001 — Bootstrap Spring Boot
- TECH-002 — Estrutura Clean Architecture
- TECH-003 — PostgreSQL local
- TECH-004 — Flyway
- TECH-005 — PostgreSQL Testcontainers
- TECH-006 — Contrato de erros da API
- TECH-007 — Convenções de persistência
- TECH-008 — Convenções de testes
- TECH-009 — Observabilidade básica

## Fase 1 — Empresa e Filial

Objetivo: estabelecer o contexto empresarial utilizado pelas funcionalidades financeiras.

## Fase 2 — Cadastros auxiliares

Ordem sugerida:

1. Parceiro
2. Categoria
3. Centro de Custo
4. Conta Bancária
5. Forma Financeira

## Fase 3 — Conta Financeira

- UC-001 — Criar Conta Financeira
- UC-002 — Consultar Conta Financeira
- UC-003 — Listar Contas Financeiras
- UC-004 — Alterar Conta Financeira
- UC-058 — Excluir Conta Financeira em Rascunho

## Fase 4 — Aprovação

- Configuração de aprovação
- UC-059 — Enviar Conta para Aprovação
- UC-005 — Aprovar Conta Financeira
- UC-006 — Rejeitar Conta Financeira
- UC-007 — Cancelar Conta Financeira

## Fase 5 — Liquidação

Antes da primeira operação financeira crítica:

- TECH-010 — Fundação de Idempotência Financeira

Depois:

- UC-008 — Liquidar Parcela

## Fase 6 — Estorno

- UC-009 — Estornar Movimentação

## Fase 7 — Histórico

- UC-010 — Consultar Histórico da Conta

## Fase 8 — Fluxo de Caixa

- UC-049 — Consultar Fluxo de Caixa
- UC-050 — Consultar Detalhes do Fluxo

## Fase 9 — Dashboard

- UC-051 — Consultar Dashboard Financeiro
- UC-052 — Consultar Evolução Financeira

## Fase 10 — Relatórios

- UC-055 — Relatório de Contas a Pagar
- UC-056 — Relatório de Contas a Receber
- UC-057 — Relatório de Fluxo de Caixa

## Fase 11 — Notificações

Implementar depois que os eventos do núcleo financeiro estiverem consolidados.

## Fase 12 — Autorização completa

Implementar:
- UsuarioEmpresa
- UsuarioEmpresaPerfil
- Perfil
- Permissao
- AuthorizationService

## Integração final

- INT-AUTH-001 — Integração com serviço externo de autenticação

Não implementar enquanto o contrato externo não estiver documentado.
