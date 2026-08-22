package com.financeiro.history.application;

import com.financeiro.history.domain.HistoryEntry;

public interface HistoryEntryRepository {
  HistoryEntry save(HistoryEntry entry);
}
