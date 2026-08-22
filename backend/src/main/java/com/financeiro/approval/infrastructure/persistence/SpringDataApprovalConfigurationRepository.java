package com.financeiro.approval.infrastructure.persistence;

import com.financeiro.financialaccount.domain.FinancialAccountType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataApprovalConfigurationRepository
    extends JpaRepository<ApprovalConfigurationJpaEntity, Long> {
  Optional<ApprovalConfigurationJpaEntity> findByCompanyIdAndBranchIdAndFinancialAccountType(
      Long companyId, Long branchId, FinancialAccountType financialAccountType);

  Optional<ApprovalConfigurationJpaEntity> findByCompanyIdAndBranchIdIsNullAndFinancialAccountType(
      Long companyId, FinancialAccountType financialAccountType);
}
