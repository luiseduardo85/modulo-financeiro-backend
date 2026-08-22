package com.financeiro.financialmovement.infrastructure.persistence;

import com.financeiro.financialmovement.application.FinancialMovementRepository;
import com.financeiro.financialmovement.domain.FinancialMovement;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaFinancialMovementRepositoryAdapter implements FinancialMovementRepository {
  private final SpringDataFinancialMovementRepository repository;

  public JpaFinancialMovementRepositoryAdapter(SpringDataFinancialMovementRepository repository) {
    this.repository = repository;
  }

  @Override
  public FinancialMovement save(FinancialMovement movement) {
    return toDomain(repository.saveAndFlush(new FinancialMovementJpaEntity(movement)));
  }

  @Override
  public Optional<FinancialMovement> findByScopeAndId(
      Long companyId, Long financialAccountId, Long installmentId, Long movementId) {
    return repository
        .findByScopeAndId(companyId, financialAccountId, installmentId, movementId)
        .map(JpaFinancialMovementRepositoryAdapter::toDomain);
  }

  private static FinancialMovement toDomain(FinancialMovementJpaEntity entity) {
    return FinancialMovement.rehydrate(
        entity.id(),
        entity.installmentId(),
        entity.type(),
        entity.amount(),
        entity.movementDate(),
        entity.bankAccountId(),
        entity.paymentMethodId(),
        entity.originalMovementId());
  }
}
