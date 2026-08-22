package com.financeiro.financialaccount.application;

import com.financeiro.company.application.CompanyNotFoundException;
import com.financeiro.company.application.CompanyRepository;
import com.financeiro.company.application.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListFinancialAccountsByCompany {
  private final CompanyRepository companies;
  private final FinancialAccountRepository accounts;

  public ListFinancialAccountsByCompany(
      CompanyRepository companies, FinancialAccountRepository accounts) {
    this.companies = companies;
    this.accounts = accounts;
  }

  @Transactional(readOnly = true)
  public PageResult<FinancialAccountSummary> execute(
      Long companyId, FinancialAccountPageQuery query) {
    companies.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    return accounts.findSummaryPageByCompanyId(companyId, query);
  }
}
