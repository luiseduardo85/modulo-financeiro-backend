package com.financeiro.cashflow.application;

import java.util.List;

public record CashFlowProjection(List<CashFlowDay> days, CashFlowSummary summary) {}
