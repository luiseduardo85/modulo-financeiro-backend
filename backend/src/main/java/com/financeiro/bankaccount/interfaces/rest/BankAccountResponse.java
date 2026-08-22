package com.financeiro.bankaccount.interfaces.rest;

import com.financeiro.bankaccount.domain.BankAccount;

public record BankAccountResponse(
    Long id, Long companyId, Long branchId, String name, boolean active) {
  static BankAccountResponse from(BankAccount v) {
    return new BankAccountResponse(v.id(), v.companyId(), v.branchId(), v.name(), v.active());
  }
}
