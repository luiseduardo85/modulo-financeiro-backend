# Domain Model

## Aggregate principal: FinancialAccount
Tipos técnicos: `PAYABLE`, `RECEIVABLE`.

Referências:
`companyId`, `branchId`, `partnerId`, `categoryId`, `costCenterId` opcional.

Campos principais:
No FUNC-005: `id`, `companyId`, `branchId`, `type`, `partnerId`, `categoryId`,
`costCenterId`, `issueDate`, `totalAmount`, `status` e `installments`.

Comportamentos:
criar, alterarDados, adicionarParcela, removerParcela, enviarParaAprovacao, finalizarSemAprovacao, aprovar, rejeitar, cancelar, liquidarParcela, estornarMovimentacao, calcularSaldo, estaVencida.

FUNC-006 implementa os comportamentos técnicos explícitos
`submitForApproval`, `approveWithoutWorkflow`, `approve` e `reject`, sem setter
genérico de status. O campo `version` é exclusivamente de persistência.

## Approval workflow

`ApprovalConfiguration` possui `id`, `companyId`, `branchId` opcional,
`financialAccountType` e `approvalRequired`. A configuração de Branch precede
a Company-wide; ausência significa fluxo desabilitado.

`ApprovalRequest` possui `id`, `financialAccountId`, `requesterActorId` e
`status` (`PENDING`, `APPROVED`, `REJECTED`). `ApprovalDecision` possui `id`,
`approvalRequestId`, `actorId`, `decision` (`APPROVED`, `REJECTED`) e
`rejectionJustification` opcional conforme a decisão. Esses registros preservam
os ciclos obrigatórios de aprovação, sem substituir o futuro histórico completo.

## Installment
Entity interna do Aggregate FinancialAccount.
No FUNC-005: `id`, `installmentNumber`, `dueDate` e `amount`.
Saldo é derivado.

## MovimentacaoFinanceira
Tipos: PAGAMENTO, RECEBIMENTO, ESTORNO.
Campos conceituais: id, tipo, valor, dataHora, paymentMethodId, bankAccountId, usuarioId, movimentacaoOriginalId.

## HistoricoConta
Entity de domínio persistida separadamente do carregamento normal do Aggregate.

## Outros Aggregates/Entities
Company, Branch, Partner, Category, CostCenter, BankAccount, PaymentMethod, Usuario, UsuarioEmpresa, UsuarioEmpresaPerfil, Perfil, PerfilPermissao, Permissao.

## BankAccount / PaymentMethod

BankAccount e um aggregate independente com somente `id: Long`,
`companyId: Long`, `branchId: Long` opcional, `name: String` e
`active: boolean`. `companyId` e imutavel. `branchId` nulo significa uso por
todas as Branches da Company; quando informado, restringe o uso a uma unica
Branch da mesma Company.

PaymentMethod e um aggregate independente e Company-scoped com somente
`id: Long`, `companyId: Long`, `name: String` e `active: boolean`. Ele pode ser
usado futuramente em PAYABLE e RECEIVABLE e nao possui campo de tipo.

Em ambos, o nome usa `String.strip()`, e obrigatorio, nao branco, limitado a 200
caracteres e pode se repetir. A criacao e ativa; `deactivate()` e a unica
transicao deste slice. Inativos permanecem consultaveis e listaveis para
preservar referencias historicas.

## Category / CostCenter

Category e CostCenter sao aggregates independentes e pertencem a exatamente uma
Company por `companyId` imutavel. Cada um contem somente `id: Long`,
`companyId: Long`, `name: String` e `active: boolean`. O nome usa
`String.strip()`, e obrigatorio, nao branco, limitado a 200 caracteres e pode
ser duplicado. A criacao e ativa e `deactivate()` e a unica transicao deste
slice. Inativos continuam consultaveis/listaveis para preservar o historico.
Nao ha codigo, hierarquia ou tipo PAYABLE/RECEIVABLE.

## Partner

Partner é global, sem vínculo com Company, e contém somente `id: Long`,
`name: String`, `document: Document`, `roles: Set<PartnerRole>` e
`active: boolean`. `name` usa `String.strip()`, é obrigatório, não branco,
limitado a 200 caracteres e não único. Roles aceitos: `CUSTOMER` e `SUPPLIER`,
com ao menos um papel. Criação produz Partner ativo; `deactivate()` é a única
transição deste slice e não remove o registro. Partner inativo continua
consultável para preservar referências históricas.

## Company / Branch

Os nomes canônicos em código são `Company` e `Branch`. `Company` possui somente
`id: Long` e `name: String`. `Branch` possui somente `id: Long`,
`companyId: Long` e `name: String`; `companyId` é obrigatório e imutável.

Para ambos, `name` é obrigatório, tem espaços externos removidos, não pode ficar
em branco após essa normalização e possui no máximo 200 caracteres. O valor
normalizado é persistido. Nomes duplicados são permitidos; não há unicidade
global nem unicidade de nome de Branch por Company.

Company e Branch são aggregates independentes. Branch referencia Company por ID
e Company não mantém uma coleção de Branches.

## Dependências proibidas no Domain
Spring, JPA/Hibernate, PostgreSQL, Flyway, HTTP, Kafka e autenticação externa.
