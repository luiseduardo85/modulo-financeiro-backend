package com.financeiro.financialaccount.application;

public record FinancialAccountPageQuery(int page, int size, Direction direction) {
  public FinancialAccountPageQuery {
    if (page < 0) throw new InvalidFinancialAccountPageException();
    if (size < 1 || size > 100) throw new InvalidFinancialAccountPageException();
    if (direction == null) throw new InvalidFinancialAccountPageException();
  }

  public enum Direction {
    ASC,
    DESC
  }
}
