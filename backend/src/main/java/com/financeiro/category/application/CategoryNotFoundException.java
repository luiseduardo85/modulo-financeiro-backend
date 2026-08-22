package com.financeiro.category.application;

public final class CategoryNotFoundException extends RuntimeException {
  public CategoryNotFoundException(Long companyId, Long categoryId) {
    super("Category not found.");
  }
}
