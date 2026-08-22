package com.financeiro.financialmovement.infrastructure.persistence;

import com.financeiro.financialmovement.application.SettlementBalanceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaSettlementBalanceRepositoryAdapter implements SettlementBalanceRepository {
  @PersistenceContext private EntityManager entityManager;

  @Override
  public BigDecimal settledAmountByInstallmentId(Long installmentId) {
    return (BigDecimal)
        entityManager
            .createNativeQuery(
                """
                SELECT COALESCE(SUM(
                    CASE WHEN movement."type" IN ('PAYMENT', 'RECEIPT') THEN movement."amount"
                         ELSE -movement."amount"
                    END), 0)
                FROM "financialMovement" movement
                WHERE movement."installmentId" = :installmentId
                """)
            .setParameter("installmentId", installmentId)
            .getSingleResult();
  }

  @Override
  public boolean hasOpenInstallments(Long financialAccountId) {
    return Boolean.TRUE.equals(
        entityManager
            .createNativeQuery(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM "installment" installment
                    WHERE installment."financialAccountId" = :financialAccountId
                      AND installment."amount" > COALESCE((
                          SELECT SUM(
                              CASE WHEN movement."type" IN ('PAYMENT', 'RECEIPT') THEN movement."amount"
                                   ELSE -movement."amount"
                              END)
                          FROM "financialMovement" movement
                          WHERE movement."installmentId" = installment."id"
                      ), 0)
                )
                """,
                Boolean.class)
            .setParameter("financialAccountId", financialAccountId)
            .getSingleResult());
  }

  @Override
  public BigDecimal reversedAmountByOriginalMovementId(Long originalMovementId) {
    return (BigDecimal)
        entityManager
            .createNativeQuery(
                """
                SELECT COALESCE(SUM(movement."amount"), 0)
                FROM "financialMovement" movement
                WHERE movement."originalMovementId" = :originalMovementId
                """)
            .setParameter("originalMovementId", originalMovementId)
            .getSingleResult();
  }
}
