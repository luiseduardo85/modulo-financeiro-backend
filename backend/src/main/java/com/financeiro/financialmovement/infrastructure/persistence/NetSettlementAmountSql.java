package com.financeiro.financialmovement.infrastructure.persistence;

/**
 * Shared SQL fragment computing a financial movement's signed effect on a settlement balance:
 * PAYMENT/RECEIPT add to the settled amount, their reversals subtract from it. Column references
 * are unqualified, so the fragment only applies where "financialMovement" is the sole table (or
 * sole aliased occurrence of it) in scope.
 */
public final class NetSettlementAmountSql {
  public static final String NET_AMOUNT_CASE =
      "CASE WHEN \"type\" IN ('PAYMENT', 'RECEIPT') THEN \"amount\" ELSE -\"amount\" END";

  private NetSettlementAmountSql() {}
}
