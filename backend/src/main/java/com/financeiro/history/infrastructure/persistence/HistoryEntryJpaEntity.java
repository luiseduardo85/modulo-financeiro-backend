package com.financeiro.history.infrastructure.persistence;

import com.financeiro.history.domain.HistoryEntry;
import com.financeiro.history.domain.HistoryEntryType;
import jakarta.persistence.*;

@Entity
@Table(name = "`financialAccountHistory`")
class HistoryEntryJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`id`", nullable = false)
  private Long id;

  @Column(name = "`financialAccountId`", nullable = false)
  private Long financialAccountId;

  @Enumerated(EnumType.STRING)
  @Column(name = "`type`", nullable = false, length = 30)
  private HistoryEntryType type;

  @Column(name = "`actorId`", length = 128)
  private String actorId;

  protected HistoryEntryJpaEntity() {}

  HistoryEntryJpaEntity(HistoryEntry entry) {
    id = entry.id();
    financialAccountId = entry.financialAccountId();
    type = entry.type();
    actorId = entry.actorId();
  }

  HistoryEntry toDomain() {
    return HistoryEntry.rehydrate(id, financialAccountId, type, actorId);
  }
}
