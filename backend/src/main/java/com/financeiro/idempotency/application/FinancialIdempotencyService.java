package com.financeiro.idempotency.application;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialIdempotencyService {

  private final IdempotencyStore store;

  public FinancialIdempotencyService(IdempotencyStore store) {
    this.store = store;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public IdempotencyClaim claim(IdempotencyScope scope, String fingerprint) {
    if (fingerprint == null || !fingerprint.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("fingerprint must be a lowercase SHA-256 digest");
    }
    IdempotencyStore.StoredRecord record = store.claimOrFind(scope, fingerprint, Instant.now());
    if (!record.fingerprint().equals(fingerprint)) {
      throw new IdempotencyConflictException();
    }
    if (record.newlyCreated()) {
      return IdempotencyClaim.claimed(record.id());
    }
    if (record.state() == IdempotencyStore.State.COMPLETED) {
      return IdempotencyClaim.completed(record.id(), record.resultReference());
    }
    throw new IdempotencyInProgressException();
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void complete(IdempotencyClaim claim, String resultReference) {
    if (claim.outcome() != IdempotencyClaim.Outcome.CLAIMED) {
      throw new IllegalArgumentException("only a newly claimed record can be completed");
    }
    if (resultReference == null || resultReference.isBlank() || resultReference.length() > 255) {
      throw new IllegalArgumentException(
          "resultReference must contain between 1 and 255 characters");
    }
    store.complete(claim.recordId(), resultReference, Instant.now());
  }
}
