package com.financeiro.bankaccount.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataBankAccountRepository extends JpaRepository<BankAccountJpaEntity, Long> {
  Optional<BankAccountJpaEntity> findByCompanyIdAndId(Long companyId, Long id);

  Page<BankAccountJpaEntity> findByCompanyId(Long companyId, Pageable pageable);
}
