package com.financeiro.company.application;

import com.financeiro.company.domain.Company;
import java.util.Optional;

public interface CompanyRepository {
    Company save(Company company);
    Optional<Company> findById(Long id);
    PageResult<Company> findPage(PageQuery query);
}
