package com.financeiro.financialaccount.interfaces.rest;

import com.financeiro.financialaccount.application.FinancialAccountSummary;
import com.financeiro.financialaccount.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialAccountSummaryResponse(
    Long id,
    Long companyId,
    Long branchId,
    FinancialAccountType type,
    Long partnerId,
    Long categoryId,
    Long costCenterId,
    LocalDate issueDate,
    BigDecimal totalAmount,
    FinancialAccountStatus status) {
  static FinancialAccountSummaryResponse from(FinancialAccountSummary value) {
    return new FinancialAccountSummaryResponse(
        value.id(),
        value.companyId(),
        value.branchId(),
        value.type(),
        value.partnerId(),
        value.categoryId(),
        value.costCenterId(),
        value.issueDate(),
        value.totalAmount(),
        value.status());
  }
}
