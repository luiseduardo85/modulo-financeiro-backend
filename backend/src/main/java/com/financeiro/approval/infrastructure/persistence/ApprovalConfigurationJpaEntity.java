package com.financeiro.approval.infrastructure.persistence;

import com.financeiro.approval.domain.ApprovalConfiguration;
import com.financeiro.financialaccount.domain.FinancialAccountType;
import jakarta.persistence.*;

@Entity
@Table(name = "`approvalConfiguration`")
class ApprovalConfigurationJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`id`", nullable = false)
  private Long id;

  @Column(name = "`companyId`", nullable = false)
  private Long companyId;

  @Column(name = "`branchId`")
  private Long branchId;

  @Enumerated(EnumType.STRING)
  @Column(name = "`financialAccountType`", nullable = false, length = 10)
  private FinancialAccountType financialAccountType;

  @Column(name = "`approvalRequired`", nullable = false)
  private boolean approvalRequired;

  protected ApprovalConfigurationJpaEntity() {}

  ApprovalConfigurationJpaEntity(ApprovalConfiguration value) {
    id = value.id();
    companyId = value.companyId();
    branchId = value.branchId();
    financialAccountType = value.financialAccountType();
    approvalRequired = value.approvalRequired();
  }

  ApprovalConfiguration toDomain() {
    return ApprovalConfiguration.rehydrate(
        id, companyId, branchId, financialAccountType, approvalRequired);
  }
}
