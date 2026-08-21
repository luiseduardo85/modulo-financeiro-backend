package com.financeiro.idempotency.application;

public record IdempotencyClaim(long recordId, Outcome outcome, String resultReference) {

    public enum Outcome {
        CLAIMED,
        COMPLETED
    }

    public static IdempotencyClaim claimed(long recordId) {
        return new IdempotencyClaim(recordId, Outcome.CLAIMED, null);
    }

    public static IdempotencyClaim completed(long recordId, String resultReference) {
        return new IdempotencyClaim(recordId, Outcome.COMPLETED, resultReference);
    }
}
