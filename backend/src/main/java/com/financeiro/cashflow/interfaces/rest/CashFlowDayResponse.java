package com.financeiro.cashflow.interfaces.rest;

import com.financeiro.cashflow.application.CashFlowDay;
import com.financeiro.cashflow.domain.CashFlowStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CashFlowDayResponse(
    LocalDate date,
    CashFlowStatus status,
    BigDecimal forecastInflow,
    BigDecimal forecastOutflow,
    BigDecimal forecastNet,
    BigDecimal actualInflow,
    BigDecimal actualOutflow,
    BigDecimal actualNet) {
  public static CashFlowDayResponse from(CashFlowDay day) {
    return new CashFlowDayResponse(
        day.date(),
        day.status(),
        day.forecastInflow(),
        day.forecastOutflow(),
        day.forecastNet(),
        day.actualInflow(),
        day.actualOutflow(),
        day.actualNet());
  }
}
