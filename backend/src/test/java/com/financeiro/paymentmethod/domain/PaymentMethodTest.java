package com.financeiro.paymentmethod.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PaymentMethodTest {
  @Test
  void createsNormalizedActiveMethod() {
    var method = PaymentMethod.create(1L, "  PIX  ");
    assertThat(method.name()).isEqualTo("PIX");
    assertThat(method.active()).isTrue();
  }

  @Test
  void enforcesStructuralInvariants() {
    assertThatThrownBy(() -> PaymentMethod.create(null, "X"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PaymentMethod.create(0L, "X"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PaymentMethod.create(1L, " "))
        .isInstanceOf(InvalidPaymentMethodNameException.class);
    assertThatThrownBy(() -> PaymentMethod.create(1L, "X".repeat(201)))
        .isInstanceOf(InvalidPaymentMethodNameException.class);
  }

  @Test
  void deactivationIsMonotonicAndInactiveStateCanBeRehydrated() {
    var method = PaymentMethod.create(1L, "PIX");
    method.deactivate();
    method.deactivate();
    assertThat(method.active()).isFalse();
    assertThat(PaymentMethod.rehydrate(2L, 1L, "Cash", false).active()).isFalse();
  }
}
