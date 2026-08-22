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
                SELECT COALESCE(SUM(%s), 0)
                FROM "financialMovement"
                WHERE "installmentId" = :installmentId
                """
                    .formatted(NetSettlementAmountSql.NET_AMOUNT_CASE))
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
                          SELECT SUM(%s)
                          FROM "financialMovement"
                          WHERE "installmentId" = installment."id"
                      ), 0)
                )
                """
                    .formatted(NetSettlementAmountSql.NET_AMOUNT_CASE),
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
