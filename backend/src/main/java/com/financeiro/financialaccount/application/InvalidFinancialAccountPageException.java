package com.financeiro.financialaccount.application;

public final class InvalidFinancialAccountPageException extends RuntimeException {
  public InvalidFinancialAccountPageException() {
    super("Invalid financial account pagination");
  }
}
