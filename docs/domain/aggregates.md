# Aggregates

ContaFinanceira é o Aggregate Root transacional principal e contém Parcelas.

Cadastros independentes: Empresa, Filial, Parceiro, Categoria, CentroCusto, BankAccount, PaymentMethod, Usuario, Perfil e Permissao.

Aggregates independentes devem se referenciar preferencialmente por IDs.
