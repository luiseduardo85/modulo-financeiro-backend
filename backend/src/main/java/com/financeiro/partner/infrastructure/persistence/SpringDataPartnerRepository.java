package com.financeiro.partner.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPartnerRepository extends JpaRepository<PartnerJpaEntity, Long> {
  Optional<PartnerJpaEntity> findByDocument(String document);
}
