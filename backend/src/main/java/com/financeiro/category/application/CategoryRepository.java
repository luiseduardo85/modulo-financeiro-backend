package com.financeiro.category.application;

import com.financeiro.category.domain.Category;
import com.financeiro.company.application.PageQuery;
import com.financeiro.company.application.PageResult;
import java.util.Optional;

public interface CategoryRepository {
  Category save(Category category);

  Optional<Category> findByCompanyIdAndId(Long companyId, Long id);

  PageResult<Category> findPageByCompanyId(Long companyId, PageQuery query);
}
