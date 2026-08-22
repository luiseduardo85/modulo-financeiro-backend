package com.financeiro.financialaccount.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataFinancialAccountRepository
    extends JpaRepository<FinancialAccountJpaEntity, Long> {
  @EntityGraph(attributePaths = "installments")
  Optional<FinancialAccountJpaEntity> findByCompanyIdAndId(Long companyId, Long id);

  Page<FinancialAccountJpaEntity> findByCompanyId(Long companyId, Pageable pageable);
}
