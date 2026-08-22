package com.financeiro.category.interfaces.rest;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;

public record CreateCategoryRequest(@NotNull String name) {
  @JsonAnySetter
  public void rejectUnknownField(String field, Object value) {
    throw new IllegalArgumentException("Unknown field: " + field);
  }
}
