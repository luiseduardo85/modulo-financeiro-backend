package com.financeiro.costcenter.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCostCenterRepository extends JpaRepository<CostCenterJpaEntity, Long> {
  Optional<CostCenterJpaEntity> findByCompanyIdAndId(Long companyId, Long id);

  Page<CostCenterJpaEntity> findByCompanyId(Long companyId, Pageable pageable);
}
