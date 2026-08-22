package com.financeiro.approval.application;

public final class ApprovalConflictException extends RuntimeException {
  public ApprovalConflictException() {
    super("The approval workflow was concurrently modified.");
  }

  public ApprovalConflictException(Throwable cause) {
    super("The approval workflow was concurrently modified.", cause);
  }
}
