package com.financeiro.approval.infrastructure.persistence;

import com.financeiro.approval.application.ApprovalConflictException;
import com.financeiro.approval.application.ApprovalDecisionRepository;
import com.financeiro.approval.domain.ApprovalDecision;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaApprovalDecisionRepositoryAdapter implements ApprovalDecisionRepository {
  private final SpringDataApprovalDecisionRepository repository;

  public JpaApprovalDecisionRepositoryAdapter(SpringDataApprovalDecisionRepository repository) {
    this.repository = repository;
  }

  @Override
  public ApprovalDecision save(ApprovalDecision value) {
    try {
      return repository.saveAndFlush(new ApprovalDecisionJpaEntity(value)).toDomain();
    } catch (DataIntegrityViolationException exception) {
      throw new ApprovalConflictException(exception);
    }
  }
}
