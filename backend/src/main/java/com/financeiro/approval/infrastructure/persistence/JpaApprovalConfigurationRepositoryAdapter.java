package com.financeiro.approval.infrastructure.persistence;

import com.financeiro.approval.application.ApprovalConfigurationRepository;
import com.financeiro.approval.domain.ApprovalConfiguration;
import com.financeiro.financialaccount.domain.FinancialAccountType;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaApprovalConfigurationRepositoryAdapter implements ApprovalConfigurationRepository {
  private final SpringDataApprovalConfigurationRepository repository;

  public JpaApprovalConfigurationRepositoryAdapter(
      SpringDataApprovalConfigurationRepository repository) {
    this.repository = repository;
  }

  @Override
  public ApprovalConfiguration save(ApprovalConfiguration configuration) {
    return repository.saveAndFlush(new ApprovalConfigurationJpaEntity(configuration)).toDomain();
  }

  @Override
  public Optional<ApprovalConfiguration> findApplicable(
      Long companyId, Long branchId, FinancialAccountType type) {
    return repository
        .findByCompanyIdAndBranchIdAndFinancialAccountType(companyId, branchId, type)
        .or(
            () ->
                repository.findByCompanyIdAndBranchIdIsNullAndFinancialAccountType(companyId, type))
        .map(ApprovalConfigurationJpaEntity::toDomain);
  }
}
