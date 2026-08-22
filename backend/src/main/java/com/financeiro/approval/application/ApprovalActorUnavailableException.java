package com.financeiro.approval.application;

public class ApprovalActorUnavailableException extends RuntimeException {
  public ApprovalActorUnavailableException() {
    super("A trusted current actor is required for approval workflow actions.");
  }
}
