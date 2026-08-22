package com.financeiro.approval.application;

import com.financeiro.approval.domain.ApprovalDecision;

public interface ApprovalDecisionRepository {
  ApprovalDecision save(ApprovalDecision decision);
}
