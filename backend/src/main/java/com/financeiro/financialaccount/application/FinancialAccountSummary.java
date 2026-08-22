package com.financeiro.financialaccount.application;

import com.financeiro.financialaccount.domain.FinancialAccountStatus;
import com.financeiro.financialaccount.domain.FinancialAccountType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialAccountSummary(
    Long id,
    Long companyId,
    Long branchId,
    FinancialAccountType type,
    Long partnerId,
    Long categoryId,
    Long costCenterId,
    LocalDate issueDate,
    BigDecimal totalAmount,
    FinancialAccountStatus status) {}
