package com.financeiro.history.application;

import com.financeiro.financialaccount.application.FinancialAccountNotFoundException;
import com.financeiro.financialaccount.application.FinancialAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetFinancialAccountHistory {
  private final FinancialAccountRepository accounts;
  private final FinancialAccountTimelineRepository timeline;

  public GetFinancialAccountHistory(
      FinancialAccountRepository accounts, FinancialAccountTimelineRepository timeline) {
    this.accounts = accounts;
    this.timeline = timeline;
  }

  @Transactional(readOnly = true)
  public List<TimelineEntry> execute(Long companyId, Long financialAccountId) {
    accounts
        .findByCompanyIdAndId(companyId, financialAccountId)
        .orElseThrow(() -> new FinancialAccountNotFoundException(financialAccountId));
    return timeline.findTimelineByFinancialAccountId(financialAccountId);
  }
}
