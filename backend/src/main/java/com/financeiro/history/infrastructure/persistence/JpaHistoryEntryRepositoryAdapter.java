package com.financeiro.history.infrastructure.persistence;

import com.financeiro.history.application.HistoryEntryRepository;
import com.financeiro.history.domain.HistoryEntry;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaHistoryEntryRepositoryAdapter implements HistoryEntryRepository {
  private final SpringDataHistoryEntryRepository repository;

  public JpaHistoryEntryRepositoryAdapter(SpringDataHistoryEntryRepository repository) {
    this.repository = repository;
  }

  @Override
  public HistoryEntry save(HistoryEntry entry) {
    return repository.saveAndFlush(new HistoryEntryJpaEntity(entry)).toDomain();
  }
}
