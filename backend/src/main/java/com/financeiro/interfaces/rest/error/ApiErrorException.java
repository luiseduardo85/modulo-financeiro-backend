package com.financeiro.interfaces.rest.error;

import java.util.Objects;

/**
 * REST-side technical exception limited to the mappings required by TECH-006. Domain and
 * Application exceptions must not depend on this type.
 */
public final class ApiErrorException extends RuntimeException {

  private final ApiErrorType type;

  public ApiErrorException(ApiErrorType type, String safeMessage) {
    super(Objects.requireNonNull(safeMessage, "safeMessage must not be null"));
    this.type = Objects.requireNonNull(type, "type must not be null");
  }

  public ApiErrorType type() {
    return type;
  }
}
