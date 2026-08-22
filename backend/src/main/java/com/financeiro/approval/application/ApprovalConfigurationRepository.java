package com.financeiro.approval.application;

import com.financeiro.approval.domain.ApprovalConfiguration;
import com.financeiro.financialaccount.domain.FinancialAccountType;
import java.util.Optional;

public interface ApprovalConfigurationRepository {
  ApprovalConfiguration save(ApprovalConfiguration configuration);

  Optional<ApprovalConfiguration> findApplicable(
      Long companyId, Long branchId, FinancialAccountType financialAccountType);
}
