package com.financeiro.interfaces.rest.error;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void normalizesNullDetailsToEmptyList() {
        ErrorResponse response = new ErrorResponse(
                "INTERNAL_ERROR", "An internal error occurred.", null,
                Instant.parse("2026-08-21T10:00:00Z"), null);

        assertThat(response.details()).isEmpty();
    }
}
