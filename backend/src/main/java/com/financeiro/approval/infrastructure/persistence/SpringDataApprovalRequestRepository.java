package com.financeiro.approval.infrastructure.persistence;

import com.financeiro.approval.domain.ApprovalRequestStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataApprovalRequestRepository
    extends JpaRepository<ApprovalRequestJpaEntity, Long> {
  Optional<ApprovalRequestJpaEntity> findByFinancialAccountIdAndStatus(
      Long financialAccountId, ApprovalRequestStatus status);
}
