package com.financeiro.financialaccount.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Installment(Long id, int installmentNumber, LocalDate dueDate, BigDecimal amount) {
  private static final int MAX_INTEGER_DIGITS = 17;

  public Installment {
    if (id != null && id <= 0) throw new InvalidInstallmentException("id must be positive");
    if (installmentNumber <= 0)
      throw new InvalidInstallmentException("installmentNumber must be positive");
    if (dueDate == null) throw new InvalidInstallmentException("dueDate is required");
    validateMoney(amount);
  }

  public static Installment create(int installmentNumber, LocalDate dueDate, BigDecimal amount) {
    return new Installment(null, installmentNumber, dueDate, amount);
  }

  public static Installment rehydrate(
      Long id, int installmentNumber, LocalDate dueDate, BigDecimal amount) {
    if (id == null) throw new InvalidInstallmentException("id is required when rehydrating");
    return new Installment(id, installmentNumber, dueDate, amount);
  }

  static void validateMoney(BigDecimal value) {
    if (value == null || value.compareTo(BigDecimal.ZERO) <= 0)
      throw new InvalidInstallmentException("amount must be positive");
    BigDecimal effective = value.stripTrailingZeros();
    if (effective.scale() > 2)
      throw new InvalidInstallmentException("amount must have at most two decimal places");
    int integerDigits = Math.max(0, effective.precision() - effective.scale());
    if (integerDigits > MAX_INTEGER_DIGITS)
      throw new InvalidInstallmentException("amount exceeds NUMERIC(19,2)");
  }
}
