package com.financeiro.idempotency.application;

import java.time.Instant;

public interface IdempotencyStore {

  StoredRecord claimOrFind(IdempotencyScope scope, String fingerprint, Instant createdAt);

  void complete(long recordId, String resultReference, Instant completedAt);

  enum State {
    PROCESSING,
    COMPLETED
  }

  record StoredRecord(
      long id, boolean newlyCreated, String fingerprint, State state, String resultReference) {}
}
