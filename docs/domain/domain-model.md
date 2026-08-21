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
Empresa, Filial, Parceiro, Categoria, CentroCusto, ContaBancaria, FormaFinanceira, Usuario, UsuarioEmpresa, UsuarioEmpresaPerfil, Perfil, PerfilPermissao, Permissao.

## Dependências proibidas no Domain
Spring, JPA/Hibernate, PostgreSQL, Flyway, HTTP, Kafka e autenticação externa.
