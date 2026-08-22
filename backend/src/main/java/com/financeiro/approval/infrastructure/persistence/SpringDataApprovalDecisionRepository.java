package com.financeiro.approval.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataApprovalDecisionRepository
    extends JpaRepository<ApprovalDecisionJpaEntity, Long> {}
