package com.financeiro.idempotency.interfaces.rest;

import com.financeiro.interfaces.rest.error.ApiErrorException;
import com.financeiro.interfaces.rest.error.ApiErrorType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyKeyValidatorTest {

    private final IdempotencyKeyValidator validator = new IdempotencyKeyValidator();

    @Test
    void acceptsOpaqueCaseSensitiveVisibleAsciiAtLengthBoundaries() {
        assertThat(validator.validate("A")).isEqualTo("A");
        assertThat(validator.validate("a")).isEqualTo("a");
        assertThat(validator.validate("!".repeat(128))).hasSize(128);
    }

    @Test
    void rejectsMissingKeyWithStableErrorType() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOfSatisfying(ApiErrorException.class,
                        exception -> assertThat(exception.type()).isEqualTo(ApiErrorType.IDEMPOTENCY_KEY_REQUIRED));
    }

    @Test
    void rejectsBlankWhitespaceControlNonAsciiAndOversizedKeys() {
        assertInvalid("");
        assertInvalid(" ");
        assertInvalid(" key");
        assertInvalid("key ");
        assertInvalid("key\nvalue");
        assertInvalid("chave-á");
        assertInvalid("a".repeat(129));
    }

    private void assertInvalid(String value) {
        assertThatThrownBy(() -> validator.validate(value))
                .isInstanceOfSatisfying(ApiErrorException.class,
                        exception -> assertThat(exception.type()).isEqualTo(ApiErrorType.INVALID_IDEMPOTENCY_KEY));
    }
}
