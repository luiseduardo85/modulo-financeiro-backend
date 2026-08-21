package com.financeiro.category.application;

import com.financeiro.category.domain.Category;
import com.financeiro.company.application.CompanyNotFoundException;
import com.financeiro.company.application.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCategory {
  private final CompanyRepository companies;
  private final CategoryRepository categories;

  public CreateCategory(CompanyRepository companies, CategoryRepository categories) {
    this.companies = companies;
    this.categories = categories;
  }

  @Transactional
  public Category execute(Long companyId, String name) {
    companies.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    return categories.save(Category.create(companyId, name));
  }
}
