package com.financeiro.approval.application;

public final class SelfApprovalNotAllowedException extends RuntimeException {
  public SelfApprovalNotAllowedException() {
    super("The requester cannot approve their own FinancialAccount.");
  }
}
