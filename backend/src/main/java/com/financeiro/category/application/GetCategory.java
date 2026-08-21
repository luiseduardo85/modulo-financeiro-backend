package com.financeiro.category.application;

import com.financeiro.category.domain.Category;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCategory {
  private final CategoryRepository categories;

  public GetCategory(CategoryRepository categories) {
    this.categories = categories;
  }

  @Transactional(readOnly = true)
  public Category execute(Long companyId, Long categoryId) {
    return categories
        .findByCompanyIdAndId(companyId, categoryId)
        .orElseThrow(() -> new CategoryNotFoundException(companyId, categoryId));
  }
}
