package com.financeiro.approval.application;

public interface ApprovalEligibility {
  boolean canApprove(String actorId, Long companyId);
}
