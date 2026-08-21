package com.financeiro.company.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCompanyRepository extends JpaRepository<CompanyJpaEntity, Long> {}
