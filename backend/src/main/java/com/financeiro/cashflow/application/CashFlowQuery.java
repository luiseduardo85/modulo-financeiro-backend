package com.financeiro.cashflow.application;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record CashFlowQuery(Long companyId, Long branchId, LocalDate startDate, LocalDate endDate) {
  private static final long MAX_RANGE_DAYS = 366;

  public CashFlowQuery {
    if (companyId == null || companyId <= 0)
      throw new InvalidCashFlowQueryException("companyId must be positive");
    if (branchId != null && branchId <= 0)
      throw new InvalidCashFlowQueryException("branchId must be positive");
    if (startDate == null) throw new InvalidCashFlowQueryException("startDate is required");
    if (endDate == null) throw new InvalidCashFlowQueryException("endDate is required");
    if (startDate.isAfter(endDate))
      throw new InvalidCashFlowQueryException("startDate must not be after endDate");
    if (ChronoUnit.DAYS.between(startDate, endDate) > MAX_RANGE_DAYS)
      throw new InvalidCashFlowQueryException(
          "date range must not exceed " + MAX_RANGE_DAYS + " days");
  }
}
