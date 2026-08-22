package com.financeiro.approval.domain;

public final class InvalidApprovalRequestException extends RuntimeException {
  public InvalidApprovalRequestException(String message) {
    super(message);
  }
}
