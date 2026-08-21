package com.financeiro.idempotency.infrastructure.persistence;

import java.time.Instant;
import java.util.List;

import com.financeiro.idempotency.application.IdempotencyScope;
import com.financeiro.idempotency.application.IdempotencyStore;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

@Repository
@Profile("!test")
public class PostgresIdempotencyStore implements IdempotencyStore {

    private static final String CLAIM_SQL = """
            INSERT INTO "idempotencyRecord" (
                "companyId", "operation", "idempotencyKey", "fingerprint", "status", "createdAt"
            ) VALUES (:companyId, :operation, :idempotencyKey, :fingerprint, 'PROCESSING', :createdAt)
            ON CONFLICT ON CONSTRAINT "ukIdempotencyRecordScope" DO NOTHING
            RETURNING "id"
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public StoredRecord claimOrFind(IdempotencyScope scope, String fingerprint, Instant createdAt) {
        @SuppressWarnings("unchecked")
        List<Number> insertedIds = entityManager.createNativeQuery(CLAIM_SQL)
                .setParameter("companyId", scope.companyId())
                .setParameter("operation", scope.operation())
                .setParameter("idempotencyKey", scope.idempotencyKey())
                .setParameter("fingerprint", fingerprint)
                .setParameter("createdAt", createdAt)
                .getResultList();

        if (!insertedIds.isEmpty()) {
            return new StoredRecord(
                    insertedIds.getFirst().longValue(), true, fingerprint, State.PROCESSING, null);
        }

        IdempotencyRecordEntity existing = entityManager.createQuery("""
                        SELECT record FROM IdempotencyRecordEntity record
                        WHERE record.companyId = :companyId
                          AND record.operation = :operation
                          AND record.idempotencyKey = :idempotencyKey
                        """, IdempotencyRecordEntity.class)
                .setParameter("companyId", scope.companyId())
                .setParameter("operation", scope.operation())
                .setParameter("idempotencyKey", scope.idempotencyKey())
                .getSingleResult();

        State state = existing.status() == IdempotencyRecordStatus.COMPLETED
                ? State.COMPLETED
                : State.PROCESSING;
        return new StoredRecord(
                existing.id(), false, existing.fingerprint(), state, existing.resultReference());
    }

    @Override
    public void complete(long recordId, String resultReference, Instant completedAt) {
        IdempotencyRecordEntity record = entityManager.find(IdempotencyRecordEntity.class, recordId);
        if (record == null) {
            throw new IllegalStateException("idempotency record not found");
        }
        record.complete(resultReference, completedAt);
    }
}
