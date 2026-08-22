package com.financeiro.company.domain;

public final class InvalidNameException extends RuntimeException {
  public InvalidNameException(String message) {
    super(message);
  }
}
