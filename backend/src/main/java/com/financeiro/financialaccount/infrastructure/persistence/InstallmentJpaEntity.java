package com.financeiro.financialaccount.infrastructure.persistence;

import com.financeiro.financialaccount.domain.Installment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "`installment`")
class InstallmentJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`id`", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "`financialAccountId`", nullable = false)
  private FinancialAccountJpaEntity financialAccount;

  @Column(name = "`installmentNumber`", nullable = false)
  private int installmentNumber;

  @Column(name = "`dueDate`", nullable = false)
  private LocalDate dueDate;

  @Column(name = "`amount`", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  protected InstallmentJpaEntity() {}

  InstallmentJpaEntity(Installment value, FinancialAccountJpaEntity account) {
    id = value.id();
    financialAccount = account;
    installmentNumber = value.installmentNumber();
    dueDate = value.dueDate();
    amount = value.amount();
  }

  Long id() {
    return id;
  }

  int installmentNumber() {
    return installmentNumber;
  }

  LocalDate dueDate() {
    return dueDate;
  }

  BigDecimal amount() {
    return amount;
  }
}
