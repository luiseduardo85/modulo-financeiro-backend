package com.financeiro.approval.infrastructure.persistence;

import com.financeiro.approval.application.ApprovalConflictException;
import com.financeiro.approval.application.ApprovalRequestRepository;
import com.financeiro.approval.domain.ApprovalRequest;
import com.financeiro.approval.domain.ApprovalRequestStatus;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaApprovalRequestRepositoryAdapter implements ApprovalRequestRepository {
  private final SpringDataApprovalRequestRepository repository;

  public JpaApprovalRequestRepositoryAdapter(SpringDataApprovalRequestRepository repository) {
    this.repository = repository;
  }

  @Override
  public ApprovalRequest save(ApprovalRequest value) {
    try {
      ApprovalRequestJpaEntity entity;
      if (value.id() == null) {
        entity = new ApprovalRequestJpaEntity(value);
      } else {
        entity =
            repository
                .findById(value.id())
                .orElseThrow(() -> new IllegalStateException("Managed ApprovalRequest is missing"));
        entity.apply(value);
      }
      return repository.saveAndFlush(entity).toDomain();
    } catch (DataIntegrityViolationException exception) {
      throw new ApprovalConflictException(exception);
    }
  }

  @Override
  public Optional<ApprovalRequest> findPendingByFinancialAccountId(Long financialAccountId) {
    return repository
        .findByFinancialAccountIdAndStatus(financialAccountId, ApprovalRequestStatus.PENDING)
        .map(ApprovalRequestJpaEntity::toDomain);
  }
}
