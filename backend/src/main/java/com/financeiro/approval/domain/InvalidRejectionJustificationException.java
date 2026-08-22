package com.financeiro.approval.domain;

public final class InvalidRejectionJustificationException extends RuntimeException {
  public InvalidRejectionJustificationException() {
    super("Rejection justification is required and must contain at most 500 characters.");
  }
}
