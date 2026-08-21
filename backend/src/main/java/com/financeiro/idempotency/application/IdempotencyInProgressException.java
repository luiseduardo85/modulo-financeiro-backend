package com.financeiro.idempotency.application;

public final class IdempotencyInProgressException extends RuntimeException {

  public IdempotencyInProgressException() {
    super("The idempotent request is still being processed.");
  }
}
