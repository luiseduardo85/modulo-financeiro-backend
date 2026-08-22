package com.financeiro.category.application;

import com.financeiro.category.domain.Category;
import com.financeiro.company.application.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListCategoriesByCompany {
  private final CompanyRepository companies;
  private final CategoryRepository categories;

  public ListCategoriesByCompany(CompanyRepository companies, CategoryRepository categories) {
    this.companies = companies;
    this.categories = categories;
  }

  @Transactional(readOnly = true)
  public PageResult<Category> execute(Long companyId, PageQuery query) {
    companies.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    return categories.findPageByCompanyId(companyId, query);
  }
}
