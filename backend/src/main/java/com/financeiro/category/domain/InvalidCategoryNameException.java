package com.financeiro.category.domain;

public final class InvalidCategoryNameException extends RuntimeException {
  public InvalidCategoryNameException() {
    super("Nome de Category inválido.");
  }
}
