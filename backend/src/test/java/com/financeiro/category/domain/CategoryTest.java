package com.financeiro.category.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CategoryTest {
  @Test
  void createsNormalizedAndActive() {
    var value = Category.create(1L, "  Operational  ");
    assertThat(value.name()).isEqualTo("Operational");
    assertThat(value.active()).isTrue();
    assertThat(value.companyId()).isEqualTo(1L);
  }

  @Test
  void rejectsInvalidStructure() {
    assertThatThrownBy(() -> Category.create(null, "Name"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Category.create(1L, " "))
        .isInstanceOf(InvalidCategoryNameException.class);
    assertThatThrownBy(() -> Category.create(1L, "x".repeat(201)))
        .isInstanceOf(InvalidCategoryNameException.class);
  }

  @Test
  void deactivationIsMonotonicAndRehydrationSupportsInactive() {
    var value = Category.rehydrate(1L, 2L, "Historical", false);
    value.deactivate();
    value.deactivate();
    assertThat(value.active()).isFalse();
  }
}
