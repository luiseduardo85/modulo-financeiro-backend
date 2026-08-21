# UC-008 — Liquidar Parcela

Permissão: `CONTA_LIQUIDAR`.

Liquidação ocorre por parcela. Valor > 0 e <= saldo. PAGAR gera PAGAMENTO; RECEBER gera RECEBIMENTO. Todas parcelas zeradas => QUITADA.

API: POST `/api/v1/contas/{contaId}/parcelas/{parcelaId}/liquidacoes`.
