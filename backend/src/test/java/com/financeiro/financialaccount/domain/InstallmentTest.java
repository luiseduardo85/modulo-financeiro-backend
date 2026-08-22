package com.financeiro.financialaccount.domain;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InstallmentTest {
  private static final LocalDate DATE = LocalDate.of(2026, 9, 12);

  @Test
  void acceptsEffectiveTwoDecimalsAndTrailingZeros() {
    assertThat(Installment.create(1, DATE, new BigDecimal("100.500")).amount())
        .isEqualByComparingTo("100.50");
  }

  @Test
  void rejectsInvalidValues() {
    assertThatThrownBy(() -> Installment.create(0, DATE, BigDecimal.ONE))
        .isInstanceOf(InvalidInstallmentException.class);
    assertThatThrownBy(() -> Installment.create(1, null, BigDecimal.ONE))
        .isInstanceOf(InvalidInstallmentException.class);
    assertThatThrownBy(() -> Installment.create(1, DATE, BigDecimal.ZERO))
        .isInstanceOf(InvalidInstallmentException.class);
    assertThatThrownBy(() -> Installment.create(1, DATE, new BigDecimal("1.001")))
        .isInstanceOf(InvalidInstallmentException.class);
    assertThatThrownBy(() -> Installment.rehydrate(null, 1, DATE, BigDecimal.ONE))
        .isInstanceOf(InvalidInstallmentException.class);
    assertThatThrownBy(() -> Installment.create(1, DATE, null))
        .isInstanceOf(InvalidInstallmentException.class);
  }

  @Test
  void rehydratesPositiveId() {
    assertThat(Installment.rehydrate(1L, 1, DATE, BigDecimal.ONE).id()).isEqualTo(1L);
  }

  @Test
  void rejectsNonPositiveRehydrationId() {
    assertThatThrownBy(() -> Installment.rehydrate(0L, 1, DATE, BigDecimal.ONE))
        .isInstanceOf(InvalidInstallmentException.class);
    assertThatThrownBy(() -> Installment.rehydrate(-1L, 1, DATE, BigDecimal.ONE))
        .isInstanceOf(InvalidInstallmentException.class);
  }
}
