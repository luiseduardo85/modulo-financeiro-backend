package com.financeiro.financialaccount.application;

public final class CategoryInactiveException extends RuntimeException {
  public CategoryInactiveException() {
    super("Category está inativa");
  }
}
