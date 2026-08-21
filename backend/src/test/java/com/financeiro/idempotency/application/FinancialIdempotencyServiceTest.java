package com.financeiro.idempotency.application;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialIdempotencyServiceTest {

    private static final IdempotencyScope SCOPE = new IdempotencyScope(1L, "TEST_OPERATION", "key-1");
    private static final String FINGERPRINT = "a".repeat(64);

    @Test
    void returnsClaimedForNewRecord() {
        StubStore store = new StubStore(new IdempotencyStore.StoredRecord(
                10L, true, FINGERPRINT, IdempotencyStore.State.PROCESSING, null));

        IdempotencyClaim claim = new FinancialIdempotencyService(store).claim(SCOPE, FINGERPRINT);

        assertThat(claim.outcome()).isEqualTo(IdempotencyClaim.Outcome.CLAIMED);
        assertThat(claim.recordId()).isEqualTo(10L);
    }

    @Test
    void returnsCompletedReferenceForCompatibleRetry() {
        StubStore store = new StubStore(new IdempotencyStore.StoredRecord(
                10L, false, FINGERPRINT, IdempotencyStore.State.COMPLETED, "result-10"));

        IdempotencyClaim claim = new FinancialIdempotencyService(store).claim(SCOPE, FINGERPRINT);

        assertThat(claim.outcome()).isEqualTo(IdempotencyClaim.Outcome.COMPLETED);
        assertThat(claim.resultReference()).isEqualTo("result-10");
    }

    @Test
    void rejectsIncompatibleReuse() {
        StubStore store = new StubStore(new IdempotencyStore.StoredRecord(
                10L, false, "b".repeat(64), IdempotencyStore.State.COMPLETED, "result-10"));

        assertThatThrownBy(() -> new FinancialIdempotencyService(store).claim(SCOPE, FINGERPRINT))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void rejectsVisibleProcessingRecord() {
        StubStore store = new StubStore(new IdempotencyStore.StoredRecord(
                10L, false, FINGERPRINT, IdempotencyStore.State.PROCESSING, null));

        assertThatThrownBy(() -> new FinancialIdempotencyService(store).claim(SCOPE, FINGERPRINT))
                .isInstanceOf(IdempotencyInProgressException.class);
    }

    @Test
    void completesOnlyNewClaimWithNonBlankReference() {
        StubStore store = new StubStore(null);
        FinancialIdempotencyService service = new FinancialIdempotencyService(store);

        service.complete(IdempotencyClaim.claimed(10L), "result-10");

        assertThat(store.completedId).isEqualTo(10L);
        assertThat(store.completedReference).isEqualTo("result-10");
    }

    private static final class StubStore implements IdempotencyStore {
        private final StoredRecord record;
        private long completedId;
        private String completedReference;

        private StubStore(StoredRecord record) {
            this.record = record;
        }

        @Override
        public StoredRecord claimOrFind(IdempotencyScope scope, String fingerprint, Instant createdAt) {
            return record;
        }

        @Override
        public void complete(long recordId, String resultReference, Instant completedAt) {
            completedId = recordId;
            completedReference = resultReference;
        }
    }
}
