package com.financeiro.approval.domain;

public final class ApprovalRequest {
  private final Long id;
  private final Long financialAccountId;
  private final String requesterActorId;
  private ApprovalRequestStatus status;

  private ApprovalRequest(
      Long id,
      Long financialAccountId,
      String requesterActorId,
      ApprovalRequestStatus status,
      boolean rehydrating) {
    if (rehydrating && (id == null || id <= 0)) invalid("id must be positive");
    if (!rehydrating && id != null) invalid("id must be absent when creating");
    if (financialAccountId == null || financialAccountId <= 0)
      invalid("financialAccountId must be positive");
    this.requesterActorId = normalizeActor(requesterActorId);
    if (status == null) invalid("status is required");
    this.id = id;
    this.financialAccountId = financialAccountId;
    this.status = status;
  }

  public static ApprovalRequest create(Long financialAccountId, String requesterActorId) {
    return new ApprovalRequest(
        null, financialAccountId, requesterActorId, ApprovalRequestStatus.PENDING, false);
  }

  public static ApprovalRequest rehydrate(
      Long id, Long financialAccountId, String requesterActorId, ApprovalRequestStatus status) {
    return new ApprovalRequest(id, financialAccountId, requesterActorId, status, true);
  }

  public void approve() {
    requirePending();
    status = ApprovalRequestStatus.APPROVED;
  }

  public void reject() {
    requirePending();
    status = ApprovalRequestStatus.REJECTED;
  }

  private void requirePending() {
    if (status != ApprovalRequestStatus.PENDING) invalid("ApprovalRequest must be PENDING");
  }

  static String normalizeActor(String value) {
    if (value == null) invalid("actorId is required");
    String normalized = value.strip();
    if (normalized.isBlank() || normalized.length() > 128)
      invalid("actorId must be nonblank and contain at most 128 characters");
    return normalized;
  }

  private static void invalid(String message) {
    throw new InvalidApprovalRequestException(message);
  }

  public Long id() {
    return id;
  }

  public Long financialAccountId() {
    return financialAccountId;
  }

  public String requesterActorId() {
    return requesterActorId;
  }

  public ApprovalRequestStatus status() {
    return status;
  }
}
