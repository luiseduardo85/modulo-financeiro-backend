package com.financeiro.approval.infrastructure.persistence;

import com.financeiro.approval.domain.ApprovalDecision;
import com.financeiro.approval.domain.ApprovalDecisionType;
import jakarta.persistence.*;

@Entity
@Table(name = "`approvalDecision`")
class ApprovalDecisionJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`id`", nullable = false)
  private Long id;

  @Column(name = "`approvalRequestId`", nullable = false, unique = true)
  private Long approvalRequestId;

  @Column(name = "`actorId`", nullable = false, length = 128)
  private String actorId;

  @Enumerated(EnumType.STRING)
  @Column(name = "`decision`", nullable = false, length = 10)
  private ApprovalDecisionType decision;

  @Column(name = "`rejectionJustification`", length = 500)
  private String rejectionJustification;

  protected ApprovalDecisionJpaEntity() {}

  ApprovalDecisionJpaEntity(ApprovalDecision value) {
    id = value.id();
    approvalRequestId = value.approvalRequestId();
    actorId = value.actorId();
    decision = value.decision();
    rejectionJustification = value.rejectionJustification();
  }

  ApprovalDecision toDomain() {
    return ApprovalDecision.rehydrate(
        id, approvalRequestId, actorId, decision, rejectionJustification);
  }
}
