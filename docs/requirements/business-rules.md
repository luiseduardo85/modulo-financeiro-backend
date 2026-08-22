# Business Rules

## Conta Financeira
- RN-CONTA-001: toda ContaFinanceira pertence a exatamente uma empresa.
- RN-CONTA-002: filial é obrigatória.
- RN-CONTA-003: filial deve pertencer à mesma empresa.
- RN-CONTA-004: valor total deve ser maior que zero.
- RN-CONTA-005: deve existir ao menos uma Installment; a soma das Installments explícitas = `totalAmount`; número > 0 e único na conta, `amount` > 0 e `dueDate` obrigatório. O cliente fornece valores explícitos e FUNC-005 não aplica resíduo nem arredondamento automático.
- RN-CONTA-006: estados técnicos persistidos: `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `SETTLED`, `CANCELLED`.
- RN-CONTA-007: vencimento não pode ser alterado após aprovação.
- RN-CONTA-008: vencimento de conta vencida não pode ser alterado.
- RN-CONTA-009: vencimentos em sábado/domingo são permitidos e não são movidos automaticamente.
- RN-CONTA-010: renegociação de prazo/valor gera novo lançamento.
- RN-CONTA-011: `DRAFT` pode ser fisicamente excluído quando elegível; contas efetivas preservam histórico.

## Aprovação
- RN-APROVACAO-001: com fluxo aplicável, `DRAFT` -> `PENDING_APPROVAL`.
- RN-APROVACAO-002: um aprovador válido é suficiente.
- RN-APROVACAO-003: solicitante não aprova a própria conta.
- RN-APROVACAO-004: `PENDING_APPROVAL` -> `APPROVED`.
- RN-APROVACAO-005: rejeição exige justificativa e volta para `DRAFT` preservando histórico.
- RN-APROVACAO-006: sem fluxo aplicável, `DRAFT` -> `APPROVED`.
- RN-APROVACAO-007: `ApprovalConfiguration` é opt-in por Company, tipo e Branch opcional; a regra exata de Branch precede a regra Company-wide e ausência de configuração significa aprovação direta.
- RN-APROVACAO-008: cada envio que exige fluxo cria um novo `ApprovalRequest`; somente um pode permanecer `PENDING` por FinancialAccount.
- RN-APROVACAO-009: aprovação/rejeição produz uma única `ApprovalDecision` para o ciclo. Rejeição persiste justificativa normalizada, não branca, com no máximo 500 caracteres. Auto-rejeição é permitida para ator elegível.
- RN-APROVACAO-010: `ApprovalActorContext` fornece a identidade confiável e `ApprovalEligibility` verifica `CONTA_APROVAR` na Company. Identidade não é aceita do payload, rota ou header público arbitrário.
- RN-APROVACAO-011: `ApprovalRequest` e `ApprovalDecision` são dados obrigatórios do workflow. O histórico completo de FinancialAccount continua requisito final, com persistência genérica deferida ao slice History.

## Cancelamento
- RN-CANCELAMENTO-001: `CONTA_APROVAR` cobre aprovar, rejeitar e cancelar.
- RN-CANCELAMENTO-002: justificativa obrigatória.
- RN-CANCELAMENTO-003: conta com qualquer movimentação efetivada não pode ser cancelada.
- RN-CANCELAMENTO-004: `APPROVED` -> `CANCELLED`.

## Liquidação
- RN-LIQUIDACAO-001: liquidação ocorre por parcela.
- RN-LIQUIDACAO-002: valor > 0 e <= saldo.
- RN-LIQUIDACAO-003: `PAYABLE` gera pagamento; `RECEIVABLE` gera recebimento.
- RN-LIQUIDACAO-004: saldo é derivado das movimentações.
- RN-LIQUIDACAO-005: todas parcelas com saldo zero => `APPROVED` -> `SETTLED`.
- RN-LIQUIDACAO-006: cada liquidação persiste uma `FinancialMovement` imutável;
  `PAYMENT` e `RECEIPT` são os únicos tipos de FUNC-007.
- RN-LIQUIDACAO-007: BankAccount e PaymentMethod são obrigatórios, devem
  pertencer à Company e estar ativos; BankAccount restrita a Branch somente pode
  ser usada pela mesma Branch da FinancialAccount.
- RN-LIQUIDACAO-008: `movementDate` é LocalDate obrigatório, sem default ou
  limitação automática de passado/futuro.
- RN-LIQUIDACAO-009: settlement exige TECH-010 e cada operação incrementa a
  versão otimista da FinancialAccount para impedir overpayment concorrente.

## Estorno
- RN-ESTORNO-001: estorno é nova movimentação vinculada à original.
- RN-ESTORNO-002: movimentação original não é apagada.
- RN-ESTORNO-003: estorno parcial permitido.
- RN-ESTORNO-004: soma dos estornos <= valor original.
- RN-ESTORNO-005: se saldo voltar a ser positivo, `SETTLED` -> `APPROVED`.
- RN-ESTORNO-006: um reversal nunca reverte outro reversal; a movimentação
  referenciada deve ser `PAYMENT` ou `RECEIPT`.
- RN-ESTORNO-007: `FinancialAccount` deve estar `APPROVED` ou `SETTLED` para
  aceitar um reversal.
- RN-ESTORNO-008: BankAccount e PaymentMethod do reversal seguem as mesmas
  regras de FUNC-007 (obrigatórios, Company-scoped, ativos; BankAccount
  restrita a Branch somente pode ser usada pela mesma Branch da
  FinancialAccount) e podem diferir dos usados na movimentação original.
- RN-ESTORNO-009: `movementDate` do reversal é `LocalDate` obrigatório, sem
  default ou limitação automática de passado/futuro.
- RN-ESTORNO-010: reversal exige TECH-010 com operação
  `REVERSE_FINANCIAL_MOVEMENT` e reutiliza a mesma serialização otimista da
  FinancialAccount usada por FUNC-007, para impedir over-reversal concorrente.
- RN-ESTORNO-011: a movimentação efetiva nunca é apagada nem atualizada;
  reversal é sempre uma nova linha append-only.

## Situações derivadas
- RN-SITUACAO-001: vencida = vencimento anterior a hoje + saldo pendente.
- RN-SITUACAO-002: liquidação parcial/total é derivada, não status principal.

## Partner
- RN-PARCEIRO-001: Partner é global e não pertence a Company.
- RN-PARCEIRO-002: CPF e CNPJ normalizados e validados; CNPJ aceita tanto o
  formato legado numérico quanto o formato alfanumérico oficial.
- RN-PARCEIRO-003: documento único globalmente.
- RN-PARCEIRO-004: pode ser CUSTOMER, SUPPLIER ou ambos.
- RN-PARCEIRO-005: inativo não entra em novos lançamentos; histórico permanece.

## Category / CostCenter
- RN-CADASTRO-001: Category e CostCenter pertencem a exatamente uma Company.
- RN-CADASTRO-002: ambos sao compartilhados entre PAYABLE e RECEIVABLE, sem campo de tipo financeiro.
- RN-CADASTRO-003: nomes duplicados sao permitidos na mesma Company.
- RN-CADASTRO-004: inativos nao entram em novos lancamentos; referencias historicas permanecem visiveis.

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
