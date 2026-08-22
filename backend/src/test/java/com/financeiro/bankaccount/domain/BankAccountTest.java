package com.financeiro.bankaccount.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BankAccountTest {
  @Test
  void createsCompanyWideAndBranchRestrictedAccounts() {
    var companyWide = BankAccount.create(1L, null, "  Operating  ");
    var restricted = BankAccount.create(1L, 2L, "Branch account");

    assertThat(companyWide.branchId()).isNull();
    assertThat(companyWide.name()).isEqualTo("Operating");
    assertThat(companyWide.active()).isTrue();
    assertThat(restricted.branchId()).isEqualTo(2L);
  }

  @Test
  void enforcesStructuralInvariants() {
    assertThatThrownBy(() -> BankAccount.create(null, null, "X"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BankAccount.create(0L, null, "X"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BankAccount.create(1L, 0L, "X"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BankAccount.create(1L, null, " "))
        .isInstanceOf(InvalidBankAccountNameException.class);
    assertThatThrownBy(() -> BankAccount.create(1L, null, "X".repeat(201)))
        .isInstanceOf(InvalidBankAccountNameException.class);
  }

  @Test
  void deactivationIsMonotonicAndInactiveStateCanBeRehydrated() {
    var account = BankAccount.create(1L, null, "X");
    account.deactivate();
    account.deactivate();
    assertThat(account.active()).isFalse();
    assertThat(BankAccount.rehydrate(2L, 1L, null, "X", false).active()).isFalse();
  }
}
