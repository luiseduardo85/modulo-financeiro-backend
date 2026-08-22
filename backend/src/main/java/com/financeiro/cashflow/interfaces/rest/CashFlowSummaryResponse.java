package com.financeiro.cashflow.interfaces.rest;

import com.financeiro.cashflow.application.CashFlowSummary;
import java.math.BigDecimal;

public record CashFlowSummaryResponse(
    BigDecimal totalForecastInflow,
    BigDecimal totalForecastOutflow,
    BigDecimal totalForecastNet,
    BigDecimal totalActualInflow,
    BigDecimal totalActualOutflow,
    BigDecimal totalActualNet) {
  public static CashFlowSummaryResponse from(CashFlowSummary summary) {
    return new CashFlowSummaryResponse(
        summary.totalForecastInflow(),
        summary.totalForecastOutflow(),
        summary.totalForecastNet(),
        summary.totalActualInflow(),
        summary.totalActualOutflow(),
        summary.totalActualNet());
  }
}
