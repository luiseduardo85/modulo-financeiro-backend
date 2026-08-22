package com.financeiro.approval.application;

public final class ApproverNotAllowedException extends RuntimeException {
  public ApproverNotAllowedException() {
    super("The current actor is not allowed to approve or reject this FinancialAccount.");
  }
}
