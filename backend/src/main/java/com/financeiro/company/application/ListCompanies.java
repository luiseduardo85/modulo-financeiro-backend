package com.financeiro.company.application;

import com.financeiro.company.domain.Company;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListCompanies {
    private final CompanyRepository repository;
    public ListCompanies(CompanyRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public PageResult<Company> execute(PageQuery query) { return repository.findPage(query); }
}
