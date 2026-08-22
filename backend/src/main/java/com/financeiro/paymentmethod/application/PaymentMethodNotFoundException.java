package com.financeiro.paymentmethod.application;

public final class PaymentMethodNotFoundException extends RuntimeException {
  public PaymentMethodNotFoundException(Long companyId, Long id) {
    super("PaymentMethod não encontrado.");
  }
}
