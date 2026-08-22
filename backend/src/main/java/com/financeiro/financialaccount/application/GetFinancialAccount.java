package com.financeiro.financialaccount.application;

import com.financeiro.financialaccount.domain.FinancialAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetFinancialAccount {
  private final FinancialAccountRepository repository;

  public GetFinancialAccount(FinancialAccountRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public FinancialAccount execute(Long companyId, Long id) {
    return repository
        .findByCompanyIdAndId(companyId, id)
        .orElseThrow(() -> new FinancialAccountNotFoundException(id));
  }
}
