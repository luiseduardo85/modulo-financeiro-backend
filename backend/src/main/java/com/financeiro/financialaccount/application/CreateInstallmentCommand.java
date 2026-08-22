package com.financeiro.financialaccount.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInstallmentCommand(
    int installmentNumber, LocalDate dueDate, BigDecimal amount) {}
