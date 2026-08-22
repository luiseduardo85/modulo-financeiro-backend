package com.financeiro.approval.application;

public final class ApprovalWorkflowConsistencyException extends RuntimeException {
  public ApprovalWorkflowConsistencyException(Long financialAccountId) {
    super("Pending ApprovalRequest is missing for FinancialAccount " + financialAccountId);
  }
}
