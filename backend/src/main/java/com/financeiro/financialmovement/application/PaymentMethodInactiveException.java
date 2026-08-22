package com.financeiro.financialmovement.application;

public final class PaymentMethodInactiveException extends RuntimeException {
  public PaymentMethodInactiveException() {
    super("Inactive PaymentMethod cannot be used for a new settlement.");
  }
}
