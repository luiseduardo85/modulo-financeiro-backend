# Initial API Endpoints

## Conta
POST /api/v1/contas
GET /api/v1/contas
GET /api/v1/contas/{id}
PUT /api/v1/contas/{id}
DELETE /api/v1/contas/{id}
POST /api/v1/contas/{id}/enviar-aprovacao
POST /api/v1/contas/{id}/aprovar
POST /api/v1/contas/{id}/rejeitar
POST /api/v1/contas/{id}/cancelar
POST /api/v1/contas/{id}/parcelas/{parcelaId}/liquidacoes
POST /api/v1/contas/{id}/movimentacoes/{movimentacaoId}/estornar
GET /api/v1/contas/{id}/historico

Outros recursos:
parceiros, categorias, centros-custo, contas-bancarias, formas-financeiras, empresas, filiais, usuarios, perfis, configuracoes-aprovacao, fluxo-caixa, dashboard/financeiro, relatorios e me.
