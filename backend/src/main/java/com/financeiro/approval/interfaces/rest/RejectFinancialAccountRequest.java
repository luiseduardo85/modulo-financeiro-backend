package com.financeiro.approval.interfaces.rest;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public record RejectFinancialAccountRequest(String justification) {
  @JsonAnySetter
  public void rejectUnknown(String field, Object value) {
    throw new IllegalArgumentException("Unknown field: " + field);
  }
}
