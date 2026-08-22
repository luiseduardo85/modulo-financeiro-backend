package com.financeiro.approval.application;

public final class InvalidApprovalActorException extends RuntimeException {
  public InvalidApprovalActorException() {
    super("Approval actor must be nonblank and contain at most 128 characters.");
  }
}
