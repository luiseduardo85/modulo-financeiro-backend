package com.financeiro.approval.application;

import com.financeiro.approval.domain.ApprovalRequest;
import com.financeiro.financialaccount.application.FinancialAccountNotFoundException;
import com.financeiro.financialaccount.application.FinancialAccountRepository;
import com.financeiro.financialaccount.domain.FinancialAccount;
import com.financeiro.financialaccount.domain.FinancialAccountStatus;
import com.financeiro.financialaccount.domain.InvalidFinancialAccountStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmitFinancialAccountForApproval {
  private final ApprovalActorContext actors;
  private final FinancialAccountRepository accounts;
  private final ApprovalConfigurationRepository configurations;
  private final ApprovalRequestRepository requests;

  public SubmitFinancialAccountForApproval(
      ApprovalActorContext actors,
      FinancialAccountRepository accounts,
      ApprovalConfigurationRepository configurations,
      ApprovalRequestRepository requests) {
    this.actors = actors;
    this.accounts = accounts;
    this.configurations = configurations;
    this.requests = requests;
  }

  @Transactional
  public FinancialAccount execute(Long companyId, Long financialAccountId) {
    var actor = actors.currentActor();
    var account = findAccount(companyId, financialAccountId);
    requireStatus(account, FinancialAccountStatus.DRAFT);
    boolean required =
        configurations
            .findApplicable(companyId, account.branchId(), account.type())
            .map(configuration -> configuration.approvalRequired())
            .orElse(false);
    if (!required) {
      account.approveWithoutWorkflow();
      return accounts.updateStatus(account);
    }
    account.submitForApproval();
    var updated = accounts.updateStatus(account);
    requests.save(ApprovalRequest.create(updated.id(), actor.actorId()));
    return updated;
  }

  private FinancialAccount findAccount(Long companyId, Long id) {
    return accounts
        .findByCompanyIdAndId(companyId, id)
        .orElseThrow(() -> new FinancialAccountNotFoundException(id));
  }

  private static void requireStatus(
      FinancialAccount account, FinancialAccountStatus requiredStatus) {
    if (account.status() != requiredStatus) {
      throw new InvalidFinancialAccountStatusException(account.status(), requiredStatus);
    }
  }
}
