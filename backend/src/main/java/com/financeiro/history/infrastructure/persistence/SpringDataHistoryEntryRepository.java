package com.financeiro.history.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataHistoryEntryRepository extends JpaRepository<HistoryEntryJpaEntity, Long> {}
