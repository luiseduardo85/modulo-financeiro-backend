# Business Rules

## Conta Financeira
- RN-CONTA-001: toda ContaFinanceira pertence a exatamente uma empresa.
- RN-CONTA-002: filial é obrigatória.
- RN-CONTA-003: filial deve pertencer à mesma empresa.
- RN-CONTA-004: valor total deve ser maior que zero.
- RN-CONTA-005: deve existir ao menos uma parcela; soma das parcelas = valor total; parcela tem número > 0, valor > 0 e vencimento obrigatório; resíduos de arredondamento vão para a última parcela.
- RN-CONTA-006: estados persistidos: RASCUNHO, PENDENTE_APROVACAO, APROVADA, QUITADA, CANCELADA.
- RN-CONTA-007: vencimento não pode ser alterado após aprovação.
- RN-CONTA-008: vencimento de conta vencida não pode ser alterado.
- RN-CONTA-009: vencimentos em sábado/domingo são permitidos e não são movidos automaticamente.
- RN-CONTA-010: renegociação de prazo/valor gera novo lançamento.
- RN-CONTA-011: RASCUNHO pode ser fisicamente excluído quando elegível; contas efetivas preservam histórico.

## Aprovação
- RN-APROVACAO-001: com fluxo aplicável, RASCUNHO -> PENDENTE_APROVACAO.
- RN-APROVACAO-002: um aprovador válido é suficiente.
- RN-APROVACAO-003: solicitante não aprova a própria conta.
- RN-APROVACAO-004: PENDENTE_APROVACAO -> APROVADA.
- RN-APROVACAO-005: rejeição exige justificativa e volta para RASCUNHO preservando histórico.
- RN-APROVACAO-006: sem fluxo aplicável, RASCUNHO -> APROVADA.

## Cancelamento
- RN-CANCELAMENTO-001: `CONTA_APROVAR` cobre aprovar, rejeitar e cancelar.
- RN-CANCELAMENTO-002: justificativa obrigatória.
- RN-CANCELAMENTO-003: conta com qualquer movimentação efetivada não pode ser cancelada.
- RN-CANCELAMENTO-004: APROVADA -> CANCELADA.

## Liquidação
- RN-LIQUIDACAO-001: liquidação ocorre por parcela.
- RN-LIQUIDACAO-002: valor > 0 e <= saldo.
- RN-LIQUIDACAO-003: PAGAR gera PAGAMENTO; RECEBER gera RECEBIMENTO.
- RN-LIQUIDACAO-004: saldo é derivado das movimentações.
- RN-LIQUIDACAO-005: todas parcelas com saldo zero => APROVADA -> QUITADA.

## Estorno
- RN-ESTORNO-001: estorno é nova movimentação vinculada à original.
- RN-ESTORNO-002: movimentação original não é apagada.
- RN-ESTORNO-003: estorno parcial permitido.
- RN-ESTORNO-004: soma dos estornos <= valor original.
- RN-ESTORNO-005: se saldo voltar a ser positivo, QUITADA -> APROVADA.

## Situações derivadas
- RN-SITUACAO-001: vencida = vencimento anterior a hoje + saldo pendente.
- RN-SITUACAO-002: liquidação parcial/total é derivada, não status principal.

## Parceiro
- RN-PARCEIRO-001: parceiro é global.
- RN-PARCEIRO-002: CPF/CNPJ normalizado e validado.
- RN-PARCEIRO-003: documento único globalmente.
- RN-PARCEIRO-004: pode ser CLIENTE, FORNECEDOR ou ambos.
- RN-PARCEIRO-005: inativo não entra em novos lançamentos; histórico permanece.

## Empresa / Filial
- RN-EMPRESA-001: usuário pode acessar múltiplas empresas.
- RN-EMPRESA-002: acesso à empresa dá acesso às suas filiais.

## BankAccount / PaymentMethod
- RN-BANCO-001: BankAccount pertence a exatamente uma Company.
- RN-BANCO-002: `branchId` nulo permite uso por todas as Branches da Company;
  quando informado, restringe a uma unica Branch da mesma Company.
- RN-BANCO-003: BankAccount inativa nao entra em novas movimentacoes; referencias
  historicas permanecem consultaveis.
- RN-BANCO-004: nomes de BankAccount podem se repetir.
- RN-PAGAMENTO-001: PaymentMethod pertence a exatamente uma Company e e compartilhado
  entre fluxos PAYABLE e RECEIVABLE, sem campo de tipo financeiro.
- RN-PAGAMENTO-002: PaymentMethod inativo nao entra em novas operacoes; referencias
  historicas permanecem consultaveis.
- RN-PAGAMENTO-003: nomes de PaymentMethod podem se repetir.

## Autorização
- RN-AUTORIZACAO-001: perfil é global.
- RN-AUTORIZACAO-002: associação usuário-perfil ocorre por empresa.
- RN-AUTORIZACAO-003: permissões efetivas resultam dos perfis ativos.
- RN-AUTORIZACAO-004: autorização exige identidade válida, acesso à empresa, permissão, estado e regras de negócio.

## Autenticação
- RN-AUTH-001: autenticação será realizada por serviço externo.
- RN-AUTH-002: não implementar até o contrato estar documentado.
