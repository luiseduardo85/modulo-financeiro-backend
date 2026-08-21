# Domain Model

## Aggregate principal: ContaFinanceira
Tipos: PAGAR, RECEBER.

Referências:
empresaId, filialId, parceiroId, categoriaId, centroCustoId.

Campos principais:
id, tipo, valorTotal, status, dataEmissao, dataCriacao, parcelas.

Comportamentos:
criar, alterarDados, adicionarParcela, removerParcela, enviarParaAprovacao, finalizarSemAprovacao, aprovar, rejeitar, cancelar, liquidarParcela, estornarMovimentacao, calcularSaldo, estaVencida.

## Parcela
Entity do Aggregate ContaFinanceira.
Campos: id, numero, valor, dataVencimento, movimentacoes.
Saldo é derivado.

## MovimentacaoFinanceira
Tipos: PAGAMENTO, RECEBIMENTO, ESTORNO.
Campos conceituais: id, tipo, valor, dataHora, formaFinanceiraId, contaBancariaId, usuarioId, movimentacaoOriginalId.

## HistoricoConta
Entity de domínio persistida separadamente do carregamento normal do Aggregate.

## Outros Aggregates/Entities
Company, Branch, Parceiro, Categoria, CentroCusto, ContaBancaria, FormaFinanceira, Usuario, UsuarioEmpresa, UsuarioEmpresaPerfil, Perfil, PerfilPermissao, Permissao.

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
