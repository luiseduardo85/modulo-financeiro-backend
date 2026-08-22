package com.financeiro.history.application;

import com.financeiro.approval.domain.ApprovalDecisionType;
import com.financeiro.financialmovement.domain.FinancialMovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TimelineEntry(
    TimelineEntryType type,
    Instant occurredAt,
    String actorId,
    ApprovalDecisionType decision,
    String rejectionJustification,
    Long movementId,
    Long originalMovementId,
    FinancialMovementType movementType,
    BigDecimal amount,
    LocalDate movementDate) {}
