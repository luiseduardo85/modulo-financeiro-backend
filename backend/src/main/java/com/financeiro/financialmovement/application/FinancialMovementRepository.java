package com.financeiro.financialmovement.application;

import com.financeiro.financialmovement.domain.FinancialMovement;
import java.util.Optional;

public interface FinancialMovementRepository {
  FinancialMovement save(FinancialMovement movement);

  Optional<FinancialMovement> findByScopeAndId(
      Long companyId, Long financialAccountId, Long installmentId, Long movementId);
}
