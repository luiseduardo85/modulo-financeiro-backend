package com.financeiro.bankaccount.interfaces.rest;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateBankAccountRequest(@NotNull String name, @Positive Long branchId) {
  @JsonAnySetter
  public void rejectUnknownField(String field, Object value) {
    throw new IllegalArgumentException("Unknown field: " + field);
  }
}
