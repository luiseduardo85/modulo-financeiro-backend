package com.financeiro.history.domain;

public final class InvalidHistoryEntryException extends RuntimeException {
  public InvalidHistoryEntryException(String message) {
    super(message);
  }
}
