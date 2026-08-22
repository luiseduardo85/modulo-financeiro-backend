package com.financeiro.approval.application;

import com.financeiro.approval.domain.ApprovalRequest;
import java.util.Optional;

public interface ApprovalRequestRepository {
  ApprovalRequest save(ApprovalRequest request);

  Optional<ApprovalRequest> findPendingByFinancialAccountId(Long financialAccountId);
}
