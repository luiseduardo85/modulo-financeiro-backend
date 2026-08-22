package com.financeiro.cashflow.application;

import java.util.List;

public interface CashFlowRepository {
  /** At most one row per distinct date; {@link GetCashFlow} relies on this to merge by date. */
  List<CashFlowAggregateRow> forecastByCompany(CashFlowQuery query);

  /** At most one row per distinct date; {@link GetCashFlow} relies on this to merge by date. */
  List<CashFlowAggregateRow> actualByCompany(CashFlowQuery query);
}
