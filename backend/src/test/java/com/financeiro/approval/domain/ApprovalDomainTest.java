package com.financeiro.approval.domain;

import static org.assertj.core.api.Assertions.*;

import com.financeiro.financialaccount.domain.FinancialAccountType;
import org.junit.jupiter.api.Test;

class ApprovalDomainTest {
  @Test
  void validatesApprovalConfigurationIdentityScopeAndType() {
    var value = ApprovalConfiguration.create(1L, null, FinancialAccountType.PAYABLE, true);
    assertThat(value.approvalRequired()).isTrue();
    assertThat(value.branchId()).isNull();
    assertThatThrownBy(
            () -> ApprovalConfiguration.create(0L, null, FinancialAccountType.PAYABLE, true))
        .isInstanceOf(InvalidApprovalConfigurationException.class);
    assertThatThrownBy(
            () -> ApprovalConfiguration.create(1L, 0L, FinancialAccountType.PAYABLE, true))
        .isInstanceOf(InvalidApprovalConfigurationException.class);
    assertThatThrownBy(() -> ApprovalConfiguration.create(1L, null, null, true))
        .isInstanceOf(InvalidApprovalConfigurationException.class);
    assertThatThrownBy(
            () -> ApprovalConfiguration.rehydrate(0L, 1L, null, FinancialAccountType.PAYABLE, true))
        .isInstanceOf(InvalidApprovalConfigurationException.class);
  }

  @Test
  void normalizesRequesterAndTransitionsRequestOnce() {
    var request = ApprovalRequest.create(10L, "  requester  ");
    assertThat(request.requesterActorId()).isEqualTo("requester");
    assertThat(request.status()).isEqualTo(ApprovalRequestStatus.PENDING);
    request.approve();
    assertThat(request.status()).isEqualTo(ApprovalRequestStatus.APPROVED);
    assertThatThrownBy(request::reject).isInstanceOf(InvalidApprovalRequestException.class);
    assertThatThrownBy(() -> ApprovalRequest.create(10L, " "))
        .isInstanceOf(InvalidApprovalRequestException.class);
    assertThatThrownBy(() -> ApprovalRequest.create(10L, "a".repeat(129)))
        .isInstanceOf(InvalidApprovalRequestException.class);
  }

  @Test
  void rejectsEveryInvalidApprovalRequestBoundary() {
    assertThatThrownBy(
            () -> ApprovalRequest.rehydrate(0L, 10L, "actor", ApprovalRequestStatus.PENDING))
        .isInstanceOf(InvalidApprovalRequestException.class);
    assertThatThrownBy(
            () -> ApprovalRequest.rehydrate(-1L, 10L, "actor", ApprovalRequestStatus.PENDING))
        .isInstanceOf(InvalidApprovalRequestException.class);
    for (Long accountId : new Long[] {null, 0L, -1L}) {
      assertThatThrownBy(() -> ApprovalRequest.create(accountId, "actor"))
          .isInstanceOf(InvalidApprovalRequestException.class);
    }
    assertThatThrownBy(() -> ApprovalRequest.rehydrate(1L, 10L, "actor", null))
        .isInstanceOf(InvalidApprovalRequestException.class);
    for (String actorId : new String[] {null, " ", "a".repeat(129)}) {
      assertThatThrownBy(() -> ApprovalRequest.create(10L, actorId))
          .isInstanceOf(InvalidApprovalRequestException.class);
    }
  }

  @Test
  void enforcesDecisionAndRejectionContracts() {
    var approved = ApprovalDecision.approve(20L, " approver ");
    assertThat(approved.actorId()).isEqualTo("approver");
    assertThat(approved.rejectionJustification()).isNull();

    var rejected =
        ApprovalDecision.reject(
            20L, "rejector", new RejectionJustification("  insufficient data  "));
    assertThat(rejected.rejectionJustification()).isEqualTo("insufficient data");
    assertThatThrownBy(() -> new RejectionJustification(null))
        .isInstanceOf(InvalidRejectionJustificationException.class);
    assertThatThrownBy(() -> new RejectionJustification(" "))
        .isInstanceOf(InvalidRejectionJustificationException.class);
    assertThatThrownBy(() -> new RejectionJustification("a".repeat(501)))
        .isInstanceOf(InvalidRejectionJustificationException.class);
    assertThatThrownBy(
            () -> ApprovalDecision.rehydrate(1L, 20L, "actor", ApprovalDecisionType.APPROVED, "no"))
        .isInstanceOf(InvalidApprovalDecisionException.class);
    assertThatThrownBy(() -> ApprovalDecision.approve(20L, "a".repeat(129)))
        .isInstanceOf(InvalidApprovalDecisionException.class);
  }

  @Test
  void rejectsEveryInvalidApprovalDecisionBoundary() {
    for (String actorId : new String[] {null, " ", "a".repeat(129)}) {
      assertThatThrownBy(() -> ApprovalDecision.approve(20L, actorId))
          .isInstanceOf(InvalidApprovalDecisionException.class);
    }
    assertThatThrownBy(() -> ApprovalDecision.rehydrate(1L, 20L, "actor", null, null))
        .isInstanceOf(InvalidApprovalDecisionException.class);
    for (Long id : new Long[] {null, 0L, -1L}) {
      assertThatThrownBy(
              () ->
                  ApprovalDecision.rehydrate(id, 20L, "actor", ApprovalDecisionType.APPROVED, null))
          .isInstanceOf(InvalidApprovalDecisionException.class);
    }
  }
}
