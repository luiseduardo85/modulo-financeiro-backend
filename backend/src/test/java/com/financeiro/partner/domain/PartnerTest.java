package com.financeiro.partner.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class PartnerTest {
  private static final Document DOC = Document.of("52998224725");

  @Test
  void normalizesNameCopiesRolesAndCreatesActive() {
    Set<PartnerRole> roles = new HashSet<>(Set.of(PartnerRole.CUSTOMER));
    Partner p = Partner.create(" Acme ", DOC, roles);
    roles.add(PartnerRole.SUPPLIER);
    assertThat(p.name()).isEqualTo("Acme");
    assertThat(p.roles()).containsExactly(PartnerRole.CUSTOMER);
    assertThat(p.active()).isTrue();
    assertThatThrownBy(() -> p.roles().add(PartnerRole.SUPPLIER))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsInvalidNameAndRoles() {
    assertThatThrownBy(() -> Partner.create(" ", DOC, Set.of(PartnerRole.CUSTOMER)))
        .isInstanceOf(InvalidPartnerNameException.class);
    assertThatThrownBy(() -> Partner.create("x".repeat(201), DOC, Set.of(PartnerRole.CUSTOMER)))
        .isInstanceOf(InvalidPartnerNameException.class);
    assertThatThrownBy(() -> Partner.create("A", DOC, Set.of()))
        .isInstanceOf(InvalidPartnerRolesException.class);
    Set<PartnerRole> roles = new HashSet<>();
    roles.add(null);
    assertThatThrownBy(() -> Partner.create("A", DOC, roles))
        .isInstanceOf(InvalidPartnerRolesException.class);
  }

  @Test
  void deactivatesAndRehydratesInactive() {
    Partner p = Partner.create("A", DOC, Set.of(PartnerRole.CUSTOMER, PartnerRole.SUPPLIER));
    p.deactivate();
    p.deactivate();
    assertThat(p.active()).isFalse();
    assertThat(Partner.rehydrate(1L, "A", DOC, Set.of(PartnerRole.SUPPLIER), false).active())
        .isFalse();
  }

  @Test
  void rehydrationRequiresAnId() {
    assertThatThrownBy(() -> Partner.rehydrate(null, "A", DOC, Set.of(PartnerRole.CUSTOMER), true))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
