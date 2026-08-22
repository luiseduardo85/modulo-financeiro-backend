package com.financeiro.history.application;

import java.util.List;

public interface FinancialAccountTimelineRepository {
  List<TimelineEntry> findTimelineByFinancialAccountId(Long financialAccountId);
}
