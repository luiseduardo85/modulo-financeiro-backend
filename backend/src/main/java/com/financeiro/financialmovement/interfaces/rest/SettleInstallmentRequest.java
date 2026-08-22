package com.financeiro.financialmovement.interfaces.rest;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SettleInstallmentRequest(
    @NotNull @Positive BigDecimal amount,
    @NotNull LocalDate movementDate,
    @NotNull @Positive Long bankAccountId,
    @NotNull @Positive Long paymentMethodId) {
  @JsonAnySetter
  public void rejectUnknown(String field, Object value) {
    throw new IllegalArgumentException("Unknown field: " + field);
  }
}
