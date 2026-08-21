# Database Constraints

Usar PK, FK, UNIQUE e NOT NULL para integridade estrutural.

Exemplos:
- parceiro.documento único globalmente;
- conta + número da parcela únicos;
- permissao.codigo único;
- associações usuário+empresa e usuário+empresa+perfil sem duplicidade;
- perfil+permissão sem duplicidade.

Não usar ON DELETE CASCADE indiscriminadamente em dados financeiros.
