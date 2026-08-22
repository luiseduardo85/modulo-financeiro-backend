package com.financeiro.financialmovement.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class FinancialMovement {

  private static final int MAX_INTEGER_DIGITS = 17;
  private final Long id;
  private final Long installmentId;
  private final FinancialMovementType type;
  private final BigDecimal amount;
  private final LocalDate movementDate;
  private final Long bankAccountId;
  private final Long paymentMethodId;

  private FinancialMovement(
      Long id,
      Long installmentId,
      FinancialMovementType type,
      BigDecimal amount,
      LocalDate movementDate,
      Long bankAccountId,
      Long paymentMethodId) {
    if (id != null && id <= 0) invalid("id must be positive");
    positive(installmentId, "installmentId");
    if (type == null) invalid("type is required");
    validateAmount(amount);
    if (movementDate == null) invalid("movementDate is required");
    positive(bankAccountId, "bankAccountId");
    positive(paymentMethodId, "paymentMethodId");
    this.id = id;
    this.installmentId = installmentId;
    this.type = type;
    this.amount = amount;
    this.movementDate = movementDate;
    this.bankAccountId = bankAccountId;
    this.paymentMethodId = paymentMethodId;
  }

  public static FinancialMovement create(
      Long installmentId,
      FinancialMovementType type,
      BigDecimal amount,
      LocalDate movementDate,
      Long bankAccountId,
      Long paymentMethodId) {
    return new FinancialMovement(
        null, installmentId, type, amount, movementDate, bankAccountId, paymentMethodId);
  }

  public static FinancialMovement rehydrate(
      Long id,
      Long installmentId,
      FinancialMovementType type,
      BigDecimal amount,
      LocalDate movementDate,
      Long bankAccountId,
      Long paymentMethodId) {
    if (id == null) invalid("id is required when rehydrating");
    return new FinancialMovement(
        id, installmentId, type, amount, movementDate, bankAccountId, paymentMethodId);
  }

  public static void validateAmount(BigDecimal value) {
    if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) invalid("amount must be positive");
    BigDecimal effective = value.stripTrailingZeros();
    if (effective.scale() > 2) invalid("amount must have at most two decimal places");
    int integerDigits = Math.max(0, effective.precision() - effective.scale());
    if (integerDigits > MAX_INTEGER_DIGITS) invalid("amount exceeds NUMERIC(19,2)");
  }

  private static void positive(Long value, String field) {
    if (value == null || value <= 0) invalid(field + " must be positive");
  }

  private static void invalid(String message) {
    throw new InvalidFinancialMovementException(message);
  }

  public Long id() {
    return id;
  }

  public Long installmentId() {
    return installmentId;
  }

  public FinancialMovementType type() {
    return type;
  }

  public BigDecimal amount() {
    return amount;
  }

  public LocalDate movementDate() {
    return movementDate;
  }

  public Long bankAccountId() {
    return bankAccountId;
  }

  public Long paymentMethodId() {
    return paymentMethodId;
  }
}
