package com.financeiro.approval.domain;

public final class InvalidApprovalDecisionException extends RuntimeException {
  public InvalidApprovalDecisionException(String message) {
    super(message);
  }
}
