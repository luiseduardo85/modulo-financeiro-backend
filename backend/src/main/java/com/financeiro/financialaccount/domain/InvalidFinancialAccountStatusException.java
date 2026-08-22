package com.financeiro.financialaccount.domain;

public final class InvalidFinancialAccountStatusException extends RuntimeException {
  public InvalidFinancialAccountStatusException(
      FinancialAccountStatus actual, FinancialAccountStatus required) {
    super(
        "FinancialAccount status "
            + actual
            + " does not allow this transition; required "
            + required);
  }
}
