package com.financeiro.interfaces.rest.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    String code,
    String message,
    List<ValidationErrorDetail> details,
    Instant timestamp,
    String traceId) {

  public ErrorResponse {
    details = details == null ? List.of() : List.copyOf(details);
  }
}
