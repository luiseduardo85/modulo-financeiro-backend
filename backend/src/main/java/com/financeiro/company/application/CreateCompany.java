package com.financeiro.company.application;

import com.financeiro.company.domain.Company;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCompany {
    private final CompanyRepository repository;
    public CreateCompany(CompanyRepository repository) { this.repository = repository; }
    @Transactional public Company execute(String name) { return repository.save(Company.create(name)); }
}
