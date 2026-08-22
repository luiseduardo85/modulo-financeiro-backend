package com.financeiro.financialaccount.interfaces.rest;

import com.financeiro.financialaccount.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinancialAccountResponse(
    Long id,
    Long companyId,
    Long branchId,
    FinancialAccountType type,
    Long partnerId,
    Long categoryId,
    Long costCenterId,
    LocalDate issueDate,
    BigDecimal totalAmount,
    FinancialAccountStatus status,
    List<InstallmentResponse> installments) {
  public static FinancialAccountResponse from(FinancialAccount value) {
    return new FinancialAccountResponse(
        value.id(),
        value.companyId(),
        value.branchId(),
        value.type(),
        value.partnerId(),
        value.categoryId(),
        value.costCenterId(),
        value.issueDate(),
        value.totalAmount(),
        value.status(),
        value.installments().stream().map(InstallmentResponse::from).toList());
  }
}
