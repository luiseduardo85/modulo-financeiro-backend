package com.financeiro.financialmovement.infrastructure.persistence;

import com.financeiro.financialmovement.domain.FinancialMovement;
import com.financeiro.financialmovement.domain.FinancialMovementType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "`financialMovement`")
class FinancialMovementJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`id`", nullable = false)
  private Long id;

  @Column(name = "`installmentId`", nullable = false)
  private Long installmentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "`type`", nullable = false, length = 10)
  private FinancialMovementType type;

  @Column(name = "`amount`", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "`movementDate`", nullable = false)
  private LocalDate movementDate;

  @Column(name = "`bankAccountId`", nullable = false)
  private Long bankAccountId;

  @Column(name = "`paymentMethodId`", nullable = false)
  private Long paymentMethodId;

  protected FinancialMovementJpaEntity() {}

  FinancialMovementJpaEntity(FinancialMovement movement) {
    id = movement.id();
    installmentId = movement.installmentId();
    type = movement.type();
    amount = movement.amount();
    movementDate = movement.movementDate();
    bankAccountId = movement.bankAccountId();
    paymentMethodId = movement.paymentMethodId();
  }

  Long id() {
    return id;
  }

  Long installmentId() {
    return installmentId;
  }

  FinancialMovementType type() {
    return type;
  }

  BigDecimal amount() {
    return amount;
  }

  LocalDate movementDate() {
    return movementDate;
  }

  Long bankAccountId() {
    return bankAccountId;
  }

  Long paymentMethodId() {
    return paymentMethodId;
  }
}
