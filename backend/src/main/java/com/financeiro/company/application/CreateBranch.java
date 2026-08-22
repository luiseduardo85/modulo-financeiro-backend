package com.financeiro.company.application;

import com.financeiro.company.domain.Branch;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateBranch {
  private final CompanyRepository companies;
  private final BranchRepository branches;

  public CreateBranch(CompanyRepository companies, BranchRepository branches) {
    this.companies = companies;
    this.branches = branches;
  }

  @Transactional
  public Branch execute(Long companyId, String name) {
    companies.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    return branches.save(Branch.create(companyId, name));
  }
}
