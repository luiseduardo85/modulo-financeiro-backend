package com.financeiro.history.domain;

public final class HistoryEntry {
  private final Long id;
  private final Long financialAccountId;
  private final HistoryEntryType type;
  private final String actorId;

  private HistoryEntry(
      Long id,
      Long financialAccountId,
      HistoryEntryType type,
      String actorId,
      boolean rehydrating) {
    if (rehydrating && (id == null || id <= 0)) invalid("id must be positive");
    if (!rehydrating && id != null) invalid("id must be absent when creating");
    if (financialAccountId == null || financialAccountId <= 0)
      invalid("financialAccountId must be positive");
    if (type == null) invalid("type is required");
    if (type == HistoryEntryType.CREATED && actorId != null)
      invalid("actorId must be absent for CREATED");
    if (type == HistoryEntryType.APPROVED_WITHOUT_WORKFLOW) {
      actorId = normalizeActor(actorId);
    }
    this.id = id;
    this.financialAccountId = financialAccountId;
    this.type = type;
    this.actorId = actorId;
  }

  public static HistoryEntry created(Long financialAccountId) {
    return new HistoryEntry(null, financialAccountId, HistoryEntryType.CREATED, null, false);
  }

  public static HistoryEntry approvedWithoutWorkflow(Long financialAccountId, String actorId) {
    return new HistoryEntry(
        null, financialAccountId, HistoryEntryType.APPROVED_WITHOUT_WORKFLOW, actorId, false);
  }

  public static HistoryEntry rehydrate(
      Long id, Long financialAccountId, HistoryEntryType type, String actorId) {
    return new HistoryEntry(id, financialAccountId, type, actorId, true);
  }

  private static String normalizeActor(String value) {
    if (value == null) invalid("actorId is required");
    String normalized = value.strip();
    if (normalized.isBlank() || normalized.length() > 128)
      invalid("actorId must be nonblank and contain at most 128 characters");
    return normalized;
  }

  private static void invalid(String message) {
    throw new InvalidHistoryEntryException(message);
  }

  public Long id() {
    return id;
  }

  public Long financialAccountId() {
    return financialAccountId;
  }

  public HistoryEntryType type() {
    return type;
  }

  public String actorId() {
    return actorId;
  }
}
