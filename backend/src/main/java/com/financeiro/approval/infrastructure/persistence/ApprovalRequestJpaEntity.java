package com.financeiro.approval.infrastructure.persistence;

import com.financeiro.approval.domain.ApprovalRequest;
import com.financeiro.approval.domain.ApprovalRequestStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "`approvalRequest`")
class ApprovalRequestJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`id`", nullable = false)
  private Long id;

  @Column(name = "`financialAccountId`", nullable = false)
  private Long financialAccountId;

  @Column(name = "`requesterActorId`", nullable = false, length = 128)
  private String requesterActorId;

  @Enumerated(EnumType.STRING)
  @Column(name = "`status`", nullable = false, length = 10)
  private ApprovalRequestStatus status;

  protected ApprovalRequestJpaEntity() {}

  ApprovalRequestJpaEntity(ApprovalRequest value) {
    id = value.id();
    financialAccountId = value.financialAccountId();
    requesterActorId = value.requesterActorId();
    status = value.status();
  }

  void apply(ApprovalRequest value) {
    status = value.status();
  }

  ApprovalRequest toDomain() {
    return ApprovalRequest.rehydrate(id, financialAccountId, requesterActorId, status);
  }
}
