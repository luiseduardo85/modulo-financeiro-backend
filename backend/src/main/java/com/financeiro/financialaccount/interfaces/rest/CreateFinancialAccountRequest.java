package com.financeiro.financialaccount.interfaces.rest;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.financeiro.financialaccount.domain.FinancialAccountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateFinancialAccountRequest(
    @NotNull @Positive Long branchId,
    @NotNull FinancialAccountType type,
    @NotNull @Positive Long partnerId,
    @NotNull @Positive Long categoryId,
    @Positive Long costCenterId,
    @NotNull LocalDate issueDate,
    @NotNull BigDecimal totalAmount,
    @NotEmpty List<@NotNull @Valid CreateInstallmentRequest> installments) {
  @JsonAnySetter
  public void rejectUnknownField(String field, Object value) {
    throw new IllegalArgumentException("Unknown field: " + field);
  }
}
