package com.financeiro.costcenter.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CostCenterTest {
  @Test
  void createsNormalizedAndActive() {
    var value = CostCenter.create(1L, "  Operations  ");
    assertThat(value.name()).isEqualTo("Operations");
    assertThat(value.active()).isTrue();
    assertThat(value.companyId()).isEqualTo(1L);
  }

  @Test
  void rejectsInvalidStructure() {
    assertThatThrownBy(() -> CostCenter.create(0L, "Name"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> CostCenter.create(1L, null))
        .isInstanceOf(InvalidCostCenterNameException.class);
    assertThatThrownBy(() -> CostCenter.create(1L, "x".repeat(201)))
        .isInstanceOf(InvalidCostCenterNameException.class);
  }

  @Test
  void deactivationIsMonotonicAndRehydrationSupportsInactive() {
    var value = CostCenter.rehydrate(1L, 2L, "Historical", false);
    value.deactivate();
    value.deactivate();
    assertThat(value.active()).isFalse();
  }
}
