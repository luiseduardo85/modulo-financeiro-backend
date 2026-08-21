package com.financeiro.idempotency.application;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sha256FingerprintTest {

    @Test
    void producesStableLowercaseSha256ForOrderedComponents() {
        String first = Sha256Fingerprint.fromOrderedComponents(
                List.of("v1", "operation", "target-42", "10.00"));
        String second = Sha256Fingerprint.fromOrderedComponents(
                List.of("v1", "operation", "target-42", "10.00"));

        assertThat(first).isEqualTo(second).matches("[0-9a-f]{64}");
    }

    @Test
    void changesWhenMaterialComponentChanges() {
        String first = Sha256Fingerprint.fromOrderedComponents(List.of("v1", "target-42"));
        String second = Sha256Fingerprint.fromOrderedComponents(List.of("v1", "target-43"));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void lengthPrefixMakesComponentBoundariesUnambiguous() {
        String first = Sha256Fingerprint.fromOrderedComponents(List.of("ab", "c"));
        String second = Sha256Fingerprint.fromOrderedComponents(List.of("a", "bc"));

        assertThat(first).isNotEqualTo(second);
    }
}
