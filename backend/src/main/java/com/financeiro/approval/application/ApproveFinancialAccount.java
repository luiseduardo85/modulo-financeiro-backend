package com.financeiro.approval.application;

import com.financeiro.approval.domain.ApprovalDecision;
import com.financeiro.financialaccount.application.FinancialAccountNotFoundException;
import com.financeiro.financialaccount.application.FinancialAccountRepository;
import com.financeiro.financialaccount.domain.FinancialAccount;
import com.financeiro.financialaccount.domain.FinancialAccountStatus;
import com.financeiro.financialaccount.domain.InvalidFinancialAccountStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApproveFinancialAccount {
  private final ApprovalActorContext actors;
  private final ApprovalEligibility eligibility;
  private final FinancialAccountRepository accounts;
  private final ApprovalRequestRepository requests;
  private final ApprovalDecisionRepository decisions;

  public ApproveFinancialAccount(
      ApprovalActorContext actors,
      ApprovalEligibility eligibility,
      FinancialAccountRepository accounts,
      ApprovalRequestRepository requests,
      ApprovalDecisionRepository decisions) {
    this.actors = actors;
    this.eligibility = eligibility;
    this.accounts = accounts;
    this.requests = requests;
    this.decisions = decisions;
  }

  @Transactional
  public FinancialAccount execute(Long companyId, Long financialAccountId) {
    var actor = actors.currentActor();
    var account = findAccount(companyId, financialAccountId);
    requirePending(account);
    var request =
        requests
            .findPendingByFinancialAccountId(financialAccountId)
            .orElseThrow(() -> new ApprovalWorkflowConsistencyException(financialAccountId));
    if (!eligibility.canApprove(actor.actorId(), companyId))
      throw new ApproverNotAllowedException();
    if (actor.actorId().equals(request.requesterActorId()))
      throw new SelfApprovalNotAllowedException();

    account.approve();
    request.approve();
    var updated = accounts.updateStatus(account);
    requests.save(request);
    decisions.save(ApprovalDecision.approve(request.id(), actor.actorId()));
    return updated;
  }

  private FinancialAccount findAccount(Long companyId, Long id) {
    return accounts
        .findByCompanyIdAndId(companyId, id)
        .orElseThrow(() -> new FinancialAccountNotFoundException(id));
  }

  private static void requirePending(FinancialAccount account) {
    if (account.status() != FinancialAccountStatus.PENDING_APPROVAL) {
      throw new InvalidFinancialAccountStatusException(
          account.status(), FinancialAccountStatus.PENDING_APPROVAL);
    }
  }
}
