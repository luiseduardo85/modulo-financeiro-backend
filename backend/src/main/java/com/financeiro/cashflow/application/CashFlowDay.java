package com.financeiro.cashflow.application;

import com.financeiro.cashflow.domain.CashFlowStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CashFlowDay(
    LocalDate date,
    CashFlowStatus status,
    BigDecimal forecastInflow,
    BigDecimal forecastOutflow,
    BigDecimal forecastNet,
    BigDecimal actualInflow,
    BigDecimal actualOutflow,
    BigDecimal actualNet) {}
