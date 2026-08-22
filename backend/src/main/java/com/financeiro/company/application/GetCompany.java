package com.financeiro.company.application;

import com.financeiro.company.domain.Company;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCompany {
  private final CompanyRepository repository;

  public GetCompany(CompanyRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Company execute(Long id) {
    return repository.findById(id).orElseThrow(() -> new CompanyNotFoundException(id));
  }
}
