package com.financeiro.paymentmethod.domain;

public final class InvalidPaymentMethodNameException extends RuntimeException {
  public InvalidPaymentMethodNameException() {
    super("Nome de PaymentMethod inválido.");
  }
}
