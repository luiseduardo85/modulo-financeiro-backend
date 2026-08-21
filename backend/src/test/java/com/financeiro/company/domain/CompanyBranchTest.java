package com.financeiro.company.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CompanyBranchTest {
    @Test void trimsAndPersistsNames() {
        assertThat(Company.create("  Acme  ").name()).isEqualTo("Acme");
        assertThat(Branch.create(1L, "  Head Office  ").name()).isEqualTo("Head Office");
    }
    @Test void rejectsInvalidNamesAndOwnership() {
        assertThatThrownBy(() -> Company.create("   ")).isInstanceOf(InvalidNameException.class);
        assertThatThrownBy(() -> Branch.create(1L, "x".repeat(201))).isInstanceOf(InvalidNameException.class);
        assertThatThrownBy(() -> Branch.create(0L, "Branch")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void acceptsDuplicateNamesAsIndependentEntities() {
        assertThat(Company.create("Same")).isEqualTo(Company.create("Same"));
        assertThat(Branch.create(1L, "Same").name()).isEqualTo(Branch.create(1L, "Same").name());
    }
}
