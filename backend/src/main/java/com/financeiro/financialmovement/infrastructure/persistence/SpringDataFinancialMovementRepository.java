package com.financeiro.financialmovement.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataFinancialMovementRepository
    extends JpaRepository<FinancialMovementJpaEntity, Long> {

  @Query(
      value =
          """
          SELECT movement.*
          FROM "financialMovement" movement
          JOIN "installment" installment ON installment."id" = movement."installmentId"
          JOIN "financialAccount" account ON account."id" = installment."financialAccountId"
          WHERE movement."id" = :movementId
            AND movement."installmentId" = :installmentId
            AND account."id" = :financialAccountId
            AND account."companyId" = :companyId
          """,
      nativeQuery = true)
  Optional<FinancialMovementJpaEntity> findByScopeAndId(
      @Param("companyId") Long companyId,
      @Param("financialAccountId") Long financialAccountId,
      @Param("installmentId") Long installmentId,
      @Param("movementId") Long movementId);
}
