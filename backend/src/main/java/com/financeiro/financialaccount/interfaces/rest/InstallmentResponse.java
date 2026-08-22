package com.financeiro.financialaccount.interfaces.rest;

import com.financeiro.financialaccount.domain.Installment;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentResponse(
    Long id, int installmentNumber, LocalDate dueDate, BigDecimal amount) {
  static InstallmentResponse from(Installment value) {
    return new InstallmentResponse(
        value.id(), value.installmentNumber(), value.dueDate(), value.amount());
  }
}
