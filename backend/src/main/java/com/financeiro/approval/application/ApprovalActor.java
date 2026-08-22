package com.financeiro.approval.application;

public record ApprovalActor(String actorId) {
  public ApprovalActor {
    if (actorId == null) throw new InvalidApprovalActorException();
    actorId = actorId.strip();
    if (actorId.isBlank() || actorId.length() > 128) throw new InvalidApprovalActorException();
  }
}
