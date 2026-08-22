package com.financeiro.bankaccount.application;

import com.financeiro.bankaccount.domain.BankAccount;
import com.financeiro.company.application.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListBankAccountsByCompany {
  private final CompanyRepository companies;
  private final BankAccountRepository repository;

  public ListBankAccountsByCompany(CompanyRepository companies, BankAccountRepository repository) {
    this.companies = companies;
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public PageResult<BankAccount> execute(Long companyId, PageQuery query) {
    companies.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    return repository.findPageByCompanyId(companyId, query);
  }
}
