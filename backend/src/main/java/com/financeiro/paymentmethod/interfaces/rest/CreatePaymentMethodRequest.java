package com.financeiro.paymentmethod.interfaces.rest;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentMethodRequest(@NotNull String name) {
  @JsonAnySetter
  public void rejectUnknownField(String field, Object value) {
    throw new IllegalArgumentException("Unknown field: " + field);
  }
}
