package com.financeiro.partner.interfaces.rest;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.financeiro.partner.domain.PartnerRole;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record CreatePartnerRequest(
    @NotNull String name, @NotNull String document, @NotNull Set<PartnerRole> roles) {
  @JsonAnySetter
  public void rejectUnknownField(String field, Object value) {
    throw new IllegalArgumentException("Unknown field: " + field);
  }
}
