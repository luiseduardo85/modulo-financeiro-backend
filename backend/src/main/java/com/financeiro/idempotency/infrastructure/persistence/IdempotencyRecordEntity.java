package com.financeiro.idempotency.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "`idempotencyRecord`")
class IdempotencyRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`id`", nullable = false)
    private Long id;

    @Column(name = "`companyId`", nullable = false)
    private Long companyId;

    @Column(name = "`operation`", nullable = false, length = 64)
    private String operation;

    @Column(name = "`idempotencyKey`", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "`fingerprint`", nullable = false, length = 64)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "`status`", nullable = false, length = 16)
    private IdempotencyRecordStatus status;

    @Column(name = "`resultReference`", length = 255)
    private String resultReference;

    @Column(name = "`createdAt`", nullable = false)
    private Instant createdAt;

    @Column(name = "`completedAt`")
    private Instant completedAt;

    protected IdempotencyRecordEntity() {
    }

    Long id() {
        return id;
    }

    String fingerprint() {
        return fingerprint;
    }

    IdempotencyRecordStatus status() {
        return status;
    }

    String resultReference() {
        return resultReference;
    }

    void complete(String reference, Instant completionTime) {
        if (status != IdempotencyRecordStatus.PROCESSING) {
            throw new IllegalStateException("idempotency record is not processing");
        }
        status = IdempotencyRecordStatus.COMPLETED;
        resultReference = reference;
        completedAt = completionTime;
    }
}
