package com.financeiro.bankaccount.application;

import com.financeiro.bankaccount.domain.BankAccount;
import com.financeiro.company.application.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateBankAccount {
  private final CompanyRepository companies;
  private final BranchRepository branches;
  private final BankAccountRepository accounts;

  public CreateBankAccount(
      CompanyRepository companies, BranchRepository branches, BankAccountRepository accounts) {
    this.companies = companies;
    this.branches = branches;
    this.accounts = accounts;
  }

  @Transactional
  public BankAccount execute(Long companyId, Long branchId, String name) {
    BankAccount candidate = BankAccount.create(companyId, branchId, name);
    companies.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    if (branchId != null)
      branches
          .findByCompanyIdAndId(companyId, branchId)
          .orElseThrow(() -> new BranchNotFoundException(branchId));
    return accounts.save(candidate);
  }
}
