package com.financeiro.cashflow.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashFlowAggregateRow(LocalDate date, BigDecimal inflow, BigDecimal outflow) {}
