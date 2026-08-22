package com.financeiro.approval.domain;

public final class ApprovalDecision {
  private final Long id;
  private final Long approvalRequestId;
  private final String actorId;
  private final ApprovalDecisionType decision;
  private final String rejectionJustification;

  private ApprovalDecision(
      Long id,
      Long approvalRequestId,
      String actorId,
      ApprovalDecisionType decision,
      String rejectionJustification,
      boolean rehydrating) {
    if (rehydrating && (id == null || id <= 0)) invalid("id must be positive");
    if (!rehydrating && id != null) invalid("id must be absent when creating");
    if (approvalRequestId == null || approvalRequestId <= 0)
      invalid("approvalRequestId must be positive");
    this.actorId = normalizeActor(actorId);
    if (decision == null) invalid("decision is required");
    if (decision == ApprovalDecisionType.APPROVED && rejectionJustification != null)
      invalid("approved decision must not contain rejectionJustification");
    if (decision == ApprovalDecisionType.REJECTED) {
      rejectionJustification = new RejectionJustification(rejectionJustification).value();
    }
    this.id = id;
    this.approvalRequestId = approvalRequestId;
    this.decision = decision;
    this.rejectionJustification = rejectionJustification;
  }

  public static ApprovalDecision approve(Long approvalRequestId, String actorId) {
    return new ApprovalDecision(
        null, approvalRequestId, actorId, ApprovalDecisionType.APPROVED, null, false);
  }

  public static ApprovalDecision reject(
      Long approvalRequestId, String actorId, RejectionJustification justification) {
    if (justification == null) throw new InvalidRejectionJustificationException();
    return new ApprovalDecision(
        null,
        approvalRequestId,
        actorId,
        ApprovalDecisionType.REJECTED,
        justification.value(),
        false);
  }

  public static ApprovalDecision rehydrate(
      Long id,
      Long approvalRequestId,
      String actorId,
      ApprovalDecisionType decision,
      String rejectionJustification) {
    return new ApprovalDecision(
        id, approvalRequestId, actorId, decision, rejectionJustification, true);
  }

  private static String normalizeActor(String value) {
    try {
      return ApprovalRequest.normalizeActor(value);
    } catch (InvalidApprovalRequestException exception) {
      throw new InvalidApprovalDecisionException(exception.getMessage());
    }
  }

  private static void invalid(String message) {
    throw new InvalidApprovalDecisionException(message);
  }

  public Long id() {
    return id;
  }

  public Long approvalRequestId() {
    return approvalRequestId;
  }

  public String actorId() {
    return actorId;
  }

  public ApprovalDecisionType decision() {
    return decision;
  }

  public String rejectionJustification() {
    return rejectionJustification;
  }
}
