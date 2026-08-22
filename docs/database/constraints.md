# Database Constraints

Usar PK, FK, UNIQUE e NOT NULL para integridade estrutural.

Exemplos:
- `partner.document` único globalmente e ao menos um de `customer`/`supplier` verdadeiro;
- conta + número da parcela únicos;
- permissao.codigo único;
- associações usuário+empresa e usuário+empresa+perfil sem duplicidade;
- perfil+permissão sem duplicidade.
- BankAccount possui FKs independentes para Company e Branch opcional; a
  igualdade entre a Company da conta e a Company da Branch e validada na
  aplicacao para nao distorcer a PK simples existente de Branch;
- PaymentMethod possui FK obrigatoria para Company;
- nomes de BankAccount e PaymentMethod nao sao unicos.

Não usar ON DELETE CASCADE indiscriminadamente em dados financeiros.
