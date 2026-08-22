package com.financeiro.paymentmethod.interfaces.rest;

import com.financeiro.paymentmethod.domain.PaymentMethod;

public record PaymentMethodResponse(Long id, Long companyId, String name, boolean active) {
  static PaymentMethodResponse from(PaymentMethod v) {
    return new PaymentMethodResponse(v.id(), v.companyId(), v.name(), v.active());
  }
}
