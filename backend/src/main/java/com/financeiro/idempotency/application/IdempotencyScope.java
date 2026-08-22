package com.financeiro.idempotency.application;

import java.util.Objects;

public record IdempotencyScope(long companyId, String operation, String idempotencyKey) {

  public IdempotencyScope {
    if (companyId <= 0) {
      throw new IllegalArgumentException("companyId must be positive");
    }
    Objects.requireNonNull(operation, "operation must not be null");
    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    if (operation.isBlank() || operation.length() > 64) {
      throw new IllegalArgumentException("operation must contain between 1 and 64 characters");
    }
    if (idempotencyKey.isEmpty() || idempotencyKey.length() > 128) {
      throw new IllegalArgumentException(
          "idempotencyKey must contain between 1 and 128 characters");
    }
  }
}
