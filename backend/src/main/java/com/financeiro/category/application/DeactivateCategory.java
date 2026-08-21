package com.financeiro.category.application;

import com.financeiro.category.domain.Category;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeactivateCategory {
  private final CategoryRepository categories;

  public DeactivateCategory(CategoryRepository categories) {
    this.categories = categories;
  }

  @Transactional
  public Category execute(Long companyId, Long categoryId) {
    Category category =
        categories
            .findByCompanyIdAndId(companyId, categoryId)
            .orElseThrow(() -> new CategoryNotFoundException(companyId, categoryId));
    category.deactivate();
    return categories.save(category);
  }
}
