package com.financeiro.history.infrastructure.persistence;

import com.financeiro.approval.domain.ApprovalDecisionType;
import com.financeiro.financialmovement.domain.FinancialMovementType;
import com.financeiro.history.application.FinancialAccountTimelineRepository;
import com.financeiro.history.application.TimelineEntry;
import com.financeiro.history.application.TimelineEntryType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaFinancialAccountTimelineRepositoryAdapter
    implements FinancialAccountTimelineRepository {
  @PersistenceContext private EntityManager entityManager;

  @Override
  @SuppressWarnings("unchecked")
  public List<TimelineEntry> findTimelineByFinancialAccountId(Long financialAccountId) {
    List<Row> rows = new ArrayList<>();

    for (Object[] r :
        (List<Object[]>)
            entityManager
                .createNativeQuery(
                    """
                    SELECT "id", "type", "actorId", "createdAt"
                    FROM "financialAccountHistory"
                    WHERE "financialAccountId" = :financialAccountId
                    """)
                .setParameter("financialAccountId", financialAccountId)
                .getResultList()) {
      String historyType = (String) r[1];
      TimelineEntryType type =
          "CREATED".equals(historyType)
              ? TimelineEntryType.CREATED
              : TimelineEntryType.APPROVED_WITHOUT_WORKFLOW;
      rows.add(
          new Row(
              new TimelineEntry(
                  type, toInstant(r[3]), (String) r[2], null, null, null, null, null, null, null),
              phase(type),
              toLong(r[0])));
    }

    for (Object[] r :
        (List<Object[]>)
            entityManager
                .createNativeQuery(
                    """
                    SELECT "id", "requesterActorId", "createdAt"
                    FROM "approvalRequest"
                    WHERE "financialAccountId" = :financialAccountId
                    """)
                .setParameter("financialAccountId", financialAccountId)
                .getResultList()) {
      rows.add(
          new Row(
              new TimelineEntry(
                  TimelineEntryType.SUBMITTED_FOR_APPROVAL,
                  toInstant(r[2]),
                  (String) r[1],
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null),
              phase(TimelineEntryType.SUBMITTED_FOR_APPROVAL),
              toLong(r[0])));
    }

    for (Object[] r :
        (List<Object[]>)
            entityManager
                .createNativeQuery(
                    """
                    SELECT d."id", d."actorId", d."decision", d."rejectionJustification", d."createdAt"
                    FROM "approvalDecision" d
                    JOIN "approvalRequest" req ON req."id" = d."approvalRequestId"
                    WHERE req."financialAccountId" = :financialAccountId
                    """)
                .setParameter("financialAccountId", financialAccountId)
                .getResultList()) {
      rows.add(
          new Row(
              new TimelineEntry(
                  TimelineEntryType.APPROVAL_DECISION,
                  toInstant(r[4]),
                  (String) r[1],
                  ApprovalDecisionType.valueOf((String) r[2]),
                  (String) r[3],
                  null,
                  null,
                  null,
                  null,
                  null),
              phase(TimelineEntryType.APPROVAL_DECISION),
              toLong(r[0])));
    }

    for (Object[] r :
        (List<Object[]>)
            entityManager
                .createNativeQuery(
                    """
                    SELECT m."id", m."type", m."amount", m."movementDate", m."originalMovementId", m."createdAt"
                    FROM "financialMovement" m
                    JOIN "installment" i ON i."id" = m."installmentId"
                    WHERE i."financialAccountId" = :financialAccountId
                    """)
                .setParameter("financialAccountId", financialAccountId)
                .getResultList()) {
      rows.add(
          new Row(
              new TimelineEntry(
                  TimelineEntryType.FINANCIAL_MOVEMENT,
                  toInstant(r[5]),
                  null,
                  null,
                  null,
                  toLong(r[0]),
                  toLong(r[4]),
                  FinancialMovementType.valueOf((String) r[1]),
                  (BigDecimal) r[2],
                  toLocalDate(r[3])),
              phase(TimelineEntryType.FINANCIAL_MOVEMENT),
              toLong(r[0])));
    }

    return rows.stream()
        .sorted(
            Comparator.comparing((Row row) -> row.entry().occurredAt())
                .thenComparingInt(Row::phase)
                .thenComparing(Row::sourceId))
        .map(Row::entry)
        .toList();
  }

  private static int phase(TimelineEntryType type) {
    return switch (type) {
      case CREATED -> 0;
      case SUBMITTED_FOR_APPROVAL, APPROVED_WITHOUT_WORKFLOW -> 1;
      case APPROVAL_DECISION -> 2;
      case FINANCIAL_MOVEMENT -> 3;
    };
  }

  private static Long toLong(Object value) {
    return value == null ? null : ((Number) value).longValue();
  }

  private static LocalDate toLocalDate(Object value) {
    if (value == null) return null;
    if (value instanceof LocalDate localDate) return localDate;
    if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
    throw new IllegalStateException("Unsupported date type: " + value.getClass());
  }

  private static Instant toInstant(Object value) {
    if (value == null) return null;
    if (value instanceof Instant instant) return instant;
    if (value instanceof OffsetDateTime offsetDateTime) return offsetDateTime.toInstant();
    if (value instanceof Timestamp timestamp) return timestamp.toInstant();
    throw new IllegalStateException("Unsupported timestamp type: " + value.getClass());
  }

  private record Row(TimelineEntry entry, int phase, Long sourceId) {}
}
