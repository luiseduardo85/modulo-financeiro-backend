package com.financeiro.paymentmethod.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPaymentMethodRepository extends JpaRepository<PaymentMethodJpaEntity, Long> {
  Optional<PaymentMethodJpaEntity> findByCompanyIdAndId(Long companyId, Long id);

  Page<PaymentMethodJpaEntity> findByCompanyId(Long companyId, Pageable pageable);
}
