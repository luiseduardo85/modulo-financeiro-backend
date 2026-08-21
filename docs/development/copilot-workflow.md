# Copilot Development Workflow

## Fluxo oficial

```text
Documentação
   ↓
GitHub Issue
   ↓
Copilot Plan
   ↓
Revisão humana
   ↓
Copilot Agent
   ↓
Testes
   ↓
Copilot/Code Review
   ↓
Pull Request
   ↓
Merge
```

## Plan

Antes de codificar:

1. ler `.github/copilot-instructions.md`;
2. ler o Use Case;
3. ler Business Rules relacionadas;
4. ler Domain Model;
5. ler ADRs relevantes;
6. inspecionar código existente;
7. listar arquivos afetados;
8. listar testes;
9. identificar dúvidas;
10. não escrever código.

## Agent

Depois da aprovação do plano:

1. implementar apenas o plano aprovado;
2. não implementar features futuras;
3. criar ou atualizar testes;
4. executar build;
5. executar testes;
6. validar migrations;
7. relatar desvios.

## Decisões ausentes

Se surgir uma regra não documentada:

```text
Copilot identifica
   ↓
implementação para
   ↓
decisão é tomada
   ↓
documentação atualizada
   ↓
implementação continua
```

A IA não decide silenciosamente regras de negócio.
