package com.financeiro.financialmovement.domain;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FinancialMovementTest {
  private static final LocalDate DATE = LocalDate.of(2026, 8, 22);

  @Test
  void createsPaymentAndReceiptWithoutPersistedId() {
    var payment = movement(FinancialMovementType.PAYMENT, new BigDecimal("100.500"));
    var receipt = movement(FinancialMovementType.RECEIPT, new BigDecimal("40.00"));

    assertThat(payment.id()).isNull();
    assertThat(payment.type()).isEqualTo(FinancialMovementType.PAYMENT);
    assertThat(payment.amount()).isEqualByComparingTo("100.500");
    assertThat(payment.originalMovementId()).isNull();
    assertThat(receipt.type()).isEqualTo(FinancialMovementType.RECEIPT);
  }

  @Test
  void exposesOnlyFactoriesForConstruction() {
    assertThat(Arrays.stream(FinancialMovement.class.getDeclaredConstructors()))
        .allMatch(constructor -> Modifier.isPrivate(constructor.getModifiers()));
  }

  @Test
  void rehydratesOnlyPositivePersistedId() {
    assertThat(
            FinancialMovement.rehydrate(
                    9L, 1L, FinancialMovementType.PAYMENT, BigDecimal.ONE, DATE, 2L, 3L, null)
                .id())
        .isEqualTo(9L);
    for (Long id : new Long[] {null, 0L, -1L}) {
      assertThatThrownBy(
              () ->
                  FinancialMovement.rehydrate(
                      id, 1L, FinancialMovementType.PAYMENT, BigDecimal.ONE, DATE, 2L, 3L, null))
          .isInstanceOf(InvalidFinancialMovementException.class);
    }
  }

  @Test
  void enforcesRequiredPositiveReferencesAndStructuralFields() {
    assertInvalid(() -> FinancialMovement.create(1L, null, BigDecimal.ONE, DATE, 2L, 3L));
    assertInvalid(
        () ->
            FinancialMovement.create(
                1L, FinancialMovementType.PAYMENT, BigDecimal.ONE, null, 2L, 3L));
  }

  @Test
  void requiresPositiveInstallmentId() {
    for (Long id : new Long[] {null, 0L, -1L})
      assertInvalid(
          () ->
              FinancialMovement.create(
                  id, FinancialMovementType.PAYMENT, BigDecimal.ONE, DATE, 2L, 3L));
  }

  @Test
  void requiresPositiveBankAccountId() {
    for (Long id : new Long[] {null, 0L, -1L})
      assertInvalid(
          () ->
              FinancialMovement.create(
                  1L, FinancialMovementType.PAYMENT, BigDecimal.ONE, DATE, id, 3L));
  }

  @Test
  void requiresPositivePaymentMethodId() {
    for (Long id : new Long[] {null, 0L, -1L})
      assertInvalid(
          () ->
              FinancialMovement.create(
                  1L, FinancialMovementType.PAYMENT, BigDecimal.ONE, DATE, 2L, id));
  }

  @Test
  void acceptsPastAndFutureMovementDates() {
    assertThat(
            FinancialMovement.create(
                    1L,
                    FinancialMovementType.PAYMENT,
                    BigDecimal.ONE,
                    LocalDate.of(1999, 12, 31),
                    2L,
                    3L)
                .movementDate())
        .isEqualTo(LocalDate.of(1999, 12, 31));
    assertThat(
            FinancialMovement.create(
                    1L,
                    FinancialMovementType.RECEIPT,
                    BigDecimal.ONE,
                    LocalDate.of(2099, 1, 1),
                    2L,
                    3L)
                .movementDate())
        .isEqualTo(LocalDate.of(2099, 1, 1));
  }

  @Test
  void enforcesMoneyWithoutRoundingAndUsesEffectiveScale() {
    for (String valid : new String[] {"100", "100.5", "100.50", "100.500"}) {
      assertThat(movement(FinancialMovementType.PAYMENT, new BigDecimal(valid)).amount())
          .isEqualByComparingTo(valid);
    }
    for (BigDecimal invalid :
        new BigDecimal[] {null, BigDecimal.ZERO, new BigDecimal("-1"), new BigDecimal("100.501")}) {
      assertInvalid(() -> movement(FinancialMovementType.PAYMENT, invalid));
    }
  }

  @Test
  void createRejectsReversalTypes() {
    for (FinancialMovementType type :
        new FinancialMovementType[] {
          FinancialMovementType.REVERSAL_PAYMENT, FinancialMovementType.REVERSAL_RECEIPT
        }) {
      assertInvalid(() -> FinancialMovement.create(1L, type, BigDecimal.ONE, DATE, 2L, 3L));
    }
  }

  @Test
  void createReversalRejectsNonReversalTypes() {
    for (FinancialMovementType type :
        new FinancialMovementType[] {
          FinancialMovementType.PAYMENT, FinancialMovementType.RECEIPT
        }) {
      assertInvalid(
          () -> FinancialMovement.createReversal(1L, type, BigDecimal.ONE, DATE, 2L, 3L, 9L));
    }
  }

  @Test
  void createReversalRequiresPositiveOriginalMovementId() {
    for (Long originalId : new Long[] {null, 0L, -1L}) {
      assertInvalid(
          () ->
              FinancialMovement.createReversal(
                  1L,
                  FinancialMovementType.REVERSAL_PAYMENT,
                  BigDecimal.ONE,
                  DATE,
                  2L,
                  3L,
                  originalId));
    }
  }

  @Test
  void createReversalProducesReversalMovementReferencingOriginal() {
    var reversal =
        FinancialMovement.createReversal(
            1L, FinancialMovementType.REVERSAL_PAYMENT, new BigDecimal("40.00"), DATE, 2L, 3L, 77L);

    assertThat(reversal.id()).isNull();
    assertThat(reversal.type()).isEqualTo(FinancialMovementType.REVERSAL_PAYMENT);
    assertThat(reversal.originalMovementId()).isEqualTo(77L);
    assertThat(reversal.amount()).isEqualByComparingTo("40.00");
  }

  @Test
  void rehydrateRejectsInconsistentTypeAndOriginalMovementId() {
    assertInvalid(
        () ->
            FinancialMovement.rehydrate(
                9L, 1L, FinancialMovementType.PAYMENT, BigDecimal.ONE, DATE, 2L, 3L, 77L));
    assertInvalid(
        () ->
            FinancialMovement.rehydrate(
                9L,
                1L,
                FinancialMovementType.REVERSAL_PAYMENT,
                BigDecimal.ONE,
                DATE,
                2L,
                3L,
                null));
  }

  @Test
  void rehydrateAcceptsConsistentReversal() {
    var reversal =
        FinancialMovement.rehydrate(
            9L, 1L, FinancialMovementType.REVERSAL_RECEIPT, BigDecimal.ONE, DATE, 2L, 3L, 77L);
    assertThat(reversal.originalMovementId()).isEqualTo(77L);
  }

  @Test
  void financialMovementTypeMapsToItsReversalCounterpart() {
    assertThat(FinancialMovementType.PAYMENT.reversalType())
        .isEqualTo(FinancialMovementType.REVERSAL_PAYMENT);
    assertThat(FinancialMovementType.RECEIPT.reversalType())
        .isEqualTo(FinancialMovementType.REVERSAL_RECEIPT);
    assertThatThrownBy(FinancialMovementType.REVERSAL_PAYMENT::reversalType)
        .isInstanceOf(InvalidFinancialMovementException.class);
    assertThatThrownBy(FinancialMovementType.REVERSAL_RECEIPT::reversalType)
        .isInstanceOf(InvalidFinancialMovementException.class);
  }

  @Test
  void financialMovementTypeIsReversalFlagsOnlyReversalTypes() {
    assertThat(FinancialMovementType.PAYMENT.isReversal()).isFalse();
    assertThat(FinancialMovementType.RECEIPT.isReversal()).isFalse();
    assertThat(FinancialMovementType.REVERSAL_PAYMENT.isReversal()).isTrue();
    assertThat(FinancialMovementType.REVERSAL_RECEIPT.isReversal()).isTrue();
  }

  private static FinancialMovement movement(FinancialMovementType type, BigDecimal amount) {
    return FinancialMovement.create(1L, type, amount, DATE, 2L, 3L);
  }

  private static void assertInvalid(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
    assertThatThrownBy(callable).isInstanceOf(InvalidFinancialMovementException.class);
  }
}
