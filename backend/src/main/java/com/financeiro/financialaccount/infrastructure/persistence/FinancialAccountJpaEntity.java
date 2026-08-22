package com.financeiro.financialaccount.infrastructure.persistence;

import com.financeiro.financialaccount.domain.FinancialAccount;
import com.financeiro.financialaccount.domain.FinancialAccountStatus;
import com.financeiro.financialaccount.domain.FinancialAccountType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "`financialAccount`")
class FinancialAccountJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`id`", nullable = false)
  private Long id;

  @Column(name = "`companyId`", nullable = false)
  private Long companyId;

  @Column(name = "`branchId`", nullable = false)
  private Long branchId;

  @Enumerated(EnumType.STRING)
  @Column(name = "`type`", nullable = false, length = 10)
  private FinancialAccountType type;

  @Column(name = "`partnerId`", nullable = false)
  private Long partnerId;

  @Column(name = "`categoryId`", nullable = false)
  private Long categoryId;

  @Column(name = "`costCenterId`")
  private Long costCenterId;

  @Column(name = "`issueDate`", nullable = false)
  private LocalDate issueDate;

  @Column(name = "`totalAmount`", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "`status`", nullable = false, length = 20)
  private FinancialAccountStatus status;

  @Version
  @Column(name = "`version`", nullable = false)
  private Long version;

  @OneToMany(
      mappedBy = "financialAccount",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @OrderBy("installmentNumber ASC")
  private List<InstallmentJpaEntity> installments = new ArrayList<>();

  protected FinancialAccountJpaEntity() {}

  FinancialAccountJpaEntity(FinancialAccount account) {
    id = account.id();
    companyId = account.companyId();
    branchId = account.branchId();
    type = account.type();
    partnerId = account.partnerId();
    categoryId = account.categoryId();
    costCenterId = account.costCenterId();
    issueDate = account.issueDate();
    totalAmount = account.totalAmount();
    status = account.status();
    account.installments().forEach(item -> installments.add(new InstallmentJpaEntity(item, this)));
  }

  Long id() {
    return id;
  }

  Long companyId() {
    return companyId;
  }

  Long branchId() {
    return branchId;
  }

  FinancialAccountType type() {
    return type;
  }

  Long partnerId() {
    return partnerId;
  }

  Long categoryId() {
    return categoryId;
  }

  Long costCenterId() {
    return costCenterId;
  }

  LocalDate issueDate() {
    return issueDate;
  }

  BigDecimal totalAmount() {
    return totalAmount;
  }

  FinancialAccountStatus status() {
    return status;
  }

  List<InstallmentJpaEntity> installments() {
    return List.copyOf(installments);
  }

  void applyStatus(FinancialAccountStatus newStatus) {
    status = newStatus;
  }

  Long version() {
    return version;
  }
}
