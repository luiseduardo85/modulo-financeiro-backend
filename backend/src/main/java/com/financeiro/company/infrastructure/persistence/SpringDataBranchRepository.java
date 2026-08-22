package com.financeiro.company.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataBranchRepository extends JpaRepository<BranchJpaEntity, Long> {
  Optional<BranchJpaEntity> findByCompanyIdAndId(Long companyId, Long id);

  Page<BranchJpaEntity> findByCompanyId(Long companyId, Pageable pageable);
}
