package com.financeiro.approval.application;

import com.financeiro.approval.domain.ApprovalConfiguration;
import com.financeiro.company.application.BranchNotFoundException;
import com.financeiro.company.application.BranchRepository;
import com.financeiro.company.application.CompanyNotFoundException;
import com.financeiro.company.application.CompanyRepository;
import com.financeiro.financialaccount.domain.FinancialAccountType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaveApprovalConfiguration {
  private final CompanyRepository companies;
  private final BranchRepository branches;
  private final ApprovalConfigurationRepository configurations;

  public SaveApprovalConfiguration(
      CompanyRepository companies,
      BranchRepository branches,
      ApprovalConfigurationRepository configurations) {
    this.companies = companies;
    this.branches = branches;
    this.configurations = configurations;
  }

  @Transactional
  public ApprovalConfiguration execute(
      Long companyId, Long branchId, FinancialAccountType type, boolean approvalRequired) {
    var candidate = ApprovalConfiguration.create(companyId, branchId, type, approvalRequired);
    companies.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    if (branchId != null) {
      branches
          .findByCompanyIdAndId(companyId, branchId)
          .orElseThrow(() -> new BranchNotFoundException(branchId));
    }
    return configurations.save(candidate);
  }
}
