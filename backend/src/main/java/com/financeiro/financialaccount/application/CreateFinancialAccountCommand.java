package com.financeiro.financialaccount.application;

import com.financeiro.financialaccount.domain.FinancialAccountType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateFinancialAccountCommand(
    Long companyId,
    Long branchId,
    FinancialAccountType type,
    Long partnerId,
    Long categoryId,
    Long costCenterId,
    LocalDate issueDate,
    BigDecimal totalAmount,
    List<CreateInstallmentCommand> installments) {}
