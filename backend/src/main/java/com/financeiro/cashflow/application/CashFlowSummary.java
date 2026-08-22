package com.financeiro.cashflow.application;

import java.math.BigDecimal;

public record CashFlowSummary(
    BigDecimal totalForecastInflow,
    BigDecimal totalForecastOutflow,
    BigDecimal totalForecastNet,
    BigDecimal totalActualInflow,
    BigDecimal totalActualOutflow,
    BigDecimal totalActualNet) {}
