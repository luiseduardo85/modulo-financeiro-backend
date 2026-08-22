package com.financeiro.bankaccount.application;

import com.financeiro.bankaccount.domain.BankAccount;
import com.financeiro.company.application.PageQuery;
import com.financeiro.company.application.PageResult;
import java.util.Optional;

public interface BankAccountRepository {
  BankAccount save(BankAccount value);

  Optional<BankAccount> findByCompanyIdAndId(Long companyId, Long id);

  PageResult<BankAccount> findPageByCompanyId(Long companyId, PageQuery query);
}
