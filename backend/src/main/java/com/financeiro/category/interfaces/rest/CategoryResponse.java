package com.financeiro.category.interfaces.rest;

import com.financeiro.category.domain.Category;

public record CategoryResponse(Long id, Long companyId, String name, boolean active) {
  static CategoryResponse from(Category value) {
    return new CategoryResponse(value.id(), value.companyId(), value.name(), value.active());
  }
}
