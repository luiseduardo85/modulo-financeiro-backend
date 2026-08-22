package com.financeiro.financialaccount.application;

import com.financeiro.company.application.PageResult;
import com.financeiro.financialaccount.domain.FinancialAccount;
import java.util.Optional;

public interface FinancialAccountRepository {
  FinancialAccount save(FinancialAccount account);

  FinancialAccount updateStatus(FinancialAccount account);

  Optional<FinancialAccount> findByCompanyIdAndId(Long companyId, Long financialAccountId);

  PageResult<FinancialAccountSummary> findSummaryPageByCompanyId(
      Long companyId, FinancialAccountPageQuery query);
}
