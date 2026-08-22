package com.financeiro.bankaccount.application;

import com.financeiro.bankaccount.domain.BankAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetBankAccount {
  private final BankAccountRepository repository;

  public GetBankAccount(BankAccountRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public BankAccount execute(Long companyId, Long id) {
    return repository
        .findByCompanyIdAndId(companyId, id)
        .orElseThrow(() -> new BankAccountNotFoundException(companyId, id));
  }
}
