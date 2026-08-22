package com.financeiro.history.interfaces.rest;

import com.financeiro.approval.domain.ApprovalDecisionType;
import com.financeiro.financialmovement.domain.FinancialMovementType;
import com.financeiro.history.application.TimelineEntry;
import com.financeiro.history.application.TimelineEntryType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TimelineEntryResponse(
    TimelineEntryType type,
    Instant occurredAt,
    String actorId,
    ApprovalDecisionType decision,
    String rejectionJustification,
    Long movementId,
    Long originalMovementId,
    FinancialMovementType movementType,
    BigDecimal amount,
    LocalDate movementDate) {
  public static TimelineEntryResponse from(TimelineEntry entry) {
    return new TimelineEntryResponse(
        entry.type(),
        entry.occurredAt(),
        entry.actorId(),
        entry.decision(),
        entry.rejectionJustification(),
        entry.movementId(),
        entry.originalMovementId(),
        entry.movementType(),
        entry.amount(),
        entry.movementDate());
  }
}
