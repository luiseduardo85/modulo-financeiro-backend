package com.financeiro.cashflow.interfaces.rest;

import com.financeiro.cashflow.application.CashFlowProjection;
import java.util.List;

public record CashFlowResponse(List<CashFlowDayResponse> days, CashFlowSummaryResponse summary) {
  public static CashFlowResponse from(CashFlowProjection projection) {
    return new CashFlowResponse(
        projection.days().stream().map(CashFlowDayResponse::from).toList(),
        CashFlowSummaryResponse.from(projection.summary()));
  }
}
