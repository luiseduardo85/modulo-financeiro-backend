package com.financeiro.approval.domain;

public record RejectionJustification(String value) {
  public static final int MAX_LENGTH = 500;

  public RejectionJustification {
    if (value == null) throw new InvalidRejectionJustificationException();
    value = value.strip();
    if (value.isBlank() || value.length() > MAX_LENGTH) {
      throw new InvalidRejectionJustificationException();
    }
  }
}
