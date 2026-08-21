package com.financeiro.category.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCategoryRepository extends JpaRepository<CategoryJpaEntity, Long> {
  Optional<CategoryJpaEntity> findByCompanyIdAndId(Long companyId, Long id);

  Page<CategoryJpaEntity> findByCompanyId(Long companyId, Pageable pageable);
}
