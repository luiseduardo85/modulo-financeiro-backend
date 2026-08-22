package com.financeiro.bankaccount.application;

import com.financeiro.bankaccount.domain.BankAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeactivateBankAccount {
  private final BankAccountRepository repository;

  public DeactivateBankAccount(BankAccountRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public BankAccount execute(Long companyId, Long id) {
    var value =
        repository
            .findByCompanyIdAndId(companyId, id)
            .orElseThrow(() -> new BankAccountNotFoundException(companyId, id));
    value.deactivate();
    return repository.save(value);
  }
}
