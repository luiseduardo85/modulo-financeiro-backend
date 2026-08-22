package com.financeiro.idempotency.interfaces.rest;

import com.financeiro.interfaces.rest.error.ApiErrorException;
import com.financeiro.interfaces.rest.error.ApiErrorType;
import org.springframework.stereotype.Component;

@Component
public final class IdempotencyKeyValidator {

  private static final int MAX_LENGTH = 128;

  public String validate(String value) {
    if (value == null) {
      throw new ApiErrorException(
          ApiErrorType.IDEMPOTENCY_KEY_REQUIRED,
          "The " + IdempotencyHeaders.IDEMPOTENCY_KEY + " header is required.");
    }
    if (value.isEmpty() || value.length() > MAX_LENGTH || !containsOnlyVisibleAscii(value)) {
      throw new ApiErrorException(
          ApiErrorType.INVALID_IDEMPOTENCY_KEY,
          "The " + IdempotencyHeaders.IDEMPOTENCY_KEY + " header is invalid.");
    }
    return value;
  }

  private boolean containsOnlyVisibleAscii(String value) {
    return value.chars().allMatch(character -> character >= 0x21 && character <= 0x7e);
  }
}
