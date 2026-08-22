package com.financeiro.approval.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.financeiro.approval.domain.*;
import com.financeiro.financialaccount.application.FinancialAccountRepository;
import com.financeiro.financialaccount.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ApprovalUseCasesTest {
  ApprovalActorContext actors;
  ApprovalEligibility eligibility;
  FinancialAccountRepository accounts;
  ApprovalConfigurationRepository configurations;
  ApprovalRequestRepository requests;
  ApprovalDecisionRepository decisions;

  @BeforeEach
  void setUp() {
    actors = mock(ApprovalActorContext.class);
    eligibility = mock(ApprovalEligibility.class);
    accounts = mock(FinancialAccountRepository.class);
    configurations = mock(ApprovalConfigurationRepository.class);
    requests = mock(ApprovalRequestRepository.class);
    decisions = mock(ApprovalDecisionRepository.class);
    when(actors.currentActor()).thenReturn(new ApprovalActor("requester"));
    when(accounts.updateStatus(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void submitsRequiredWorkflowAndCapturesExactActor() {
    var account = account(FinancialAccountStatus.DRAFT);
    when(accounts.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(account));
    when(configurations.findApplicable(1L, 2L, FinancialAccountType.PAYABLE))
        .thenReturn(
            Optional.of(
                ApprovalConfiguration.rehydrate(1L, 1L, 2L, FinancialAccountType.PAYABLE, true)));
    when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = submit().execute(1L, 10L);

    assertThat(result.status()).isEqualTo(FinancialAccountStatus.PENDING_APPROVAL);
    var request = ArgumentCaptor.forClass(ApprovalRequest.class);
    verify(requests).save(request.capture());
    assertThat(request.getValue().requesterActorId()).isEqualTo("requester");
    assertThat(request.getValue().status()).isEqualTo(ApprovalRequestStatus.PENDING);
    verify(accounts).updateStatus(account);
  }

  @Test
  void directApprovalUsesDisabledConfigurationOrNoConfigurationWithoutRequest() {
    for (Optional<ApprovalConfiguration> configured :
        List.of(
            Optional.of(
                ApprovalConfiguration.rehydrate(1L, 1L, null, FinancialAccountType.PAYABLE, false)),
            Optional.<ApprovalConfiguration>empty())) {
      reset(accounts, configurations, requests);
      var account = account(FinancialAccountStatus.DRAFT);
      when(accounts.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(account));
      when(accounts.updateStatus(any())).thenAnswer(invocation -> invocation.getArgument(0));
      when(configurations.findApplicable(1L, 2L, FinancialAccountType.PAYABLE))
          .thenReturn(configured);

      assertThat(submit().execute(1L, 10L).status()).isEqualTo(FinancialAccountStatus.APPROVED);
      verifyNoInteractions(requests);
    }
  }

  @Test
  void passesBranchCompanyAndTypeToSinglePrecedenceLookup() {
    var account = account(FinancialAccountStatus.DRAFT);
    when(accounts.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(account));
    when(accounts.updateStatus(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(configurations.findApplicable(1L, 2L, FinancialAccountType.PAYABLE))
        .thenReturn(Optional.empty());

    submit().execute(1L, 10L);

    verify(configurations).findApplicable(1L, 2L, FinancialAccountType.PAYABLE);
    verifyNoMoreInteractions(configurations);
  }

  @Test
  void approvesOnlyEligibleDifferentActorAndPersistsRequestDecisionAndAccount() {
    when(actors.currentActor()).thenReturn(new ApprovalActor("approver"));
    var account = account(FinancialAccountStatus.PENDING_APPROVAL);
    var request = ApprovalRequest.rehydrate(30L, 10L, "requester", ApprovalRequestStatus.PENDING);
    when(accounts.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(account));
    when(requests.findPendingByFinancialAccountId(10L)).thenReturn(Optional.of(request));
    when(eligibility.canApprove("approver", 1L)).thenReturn(true);

    var result = approve().execute(1L, 10L);

    assertThat(result.status()).isEqualTo(FinancialAccountStatus.APPROVED);
    assertThat(request.status()).isEqualTo(ApprovalRequestStatus.APPROVED);
    var decision = ArgumentCaptor.forClass(ApprovalDecision.class);
    verify(decisions).save(decision.capture());
    assertThat(decision.getValue().decision()).isEqualTo(ApprovalDecisionType.APPROVED);
    assertThat(decision.getValue().actorId()).isEqualTo("approver");
    assertThat(decision.getValue().rejectionJustification()).isNull();
  }

  @Test
  void forbidsSelfApprovalAndIneligibleActorBeforeMutation() {
    var account = account(FinancialAccountStatus.PENDING_APPROVAL);
    var request = ApprovalRequest.rehydrate(30L, 10L, "requester", ApprovalRequestStatus.PENDING);
    when(accounts.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(account));
    when(requests.findPendingByFinancialAccountId(10L)).thenReturn(Optional.of(request));
    when(eligibility.canApprove("requester", 1L)).thenReturn(true);
    assertThatThrownBy(() -> approve().execute(1L, 10L))
        .isInstanceOf(SelfApprovalNotAllowedException.class);
    assertThat(account.status()).isEqualTo(FinancialAccountStatus.PENDING_APPROVAL);
    verify(accounts, never()).updateStatus(any());

    reset(eligibility);
    when(eligibility.canApprove("requester", 1L)).thenReturn(false);
    assertThatThrownBy(() -> approve().execute(1L, 10L))
        .isInstanceOf(ApproverNotAllowedException.class);
  }

  @Test
  void rejectionAllowsRequesterAndPersistsCanonicalReason() {
    var account = account(FinancialAccountStatus.PENDING_APPROVAL);
    var request = ApprovalRequest.rehydrate(30L, 10L, "requester", ApprovalRequestStatus.PENDING);
    when(accounts.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(account));
    when(requests.findPendingByFinancialAccountId(10L)).thenReturn(Optional.of(request));
    when(eligibility.canApprove("requester", 1L)).thenReturn(true);

    var result = reject().execute(1L, 10L, "  correct the invoice  ");

    assertThat(result.status()).isEqualTo(FinancialAccountStatus.DRAFT);
    assertThat(request.status()).isEqualTo(ApprovalRequestStatus.REJECTED);
    var decision = ArgumentCaptor.forClass(ApprovalDecision.class);
    verify(decisions).save(decision.capture());
    assertThat(decision.getValue().rejectionJustification()).isEqualTo("correct the invoice");
  }

  @Test
  void invalidRejectionReasonWinsBeforeActorAndRepositories() {
    for (String value : new String[] {null, " ", "a".repeat(501)}) {
      assertThatThrownBy(() -> reject().execute(1L, 10L, value))
          .isInstanceOf(InvalidRejectionJustificationException.class);
    }
    verifyNoInteractions(actors, accounts, requests, eligibility, decisions);
  }

  @Test
  void actorUnavailableStopsEveryWorkflowBeforeRepositoryAccess() {
    when(actors.currentActor()).thenThrow(new ApprovalActorUnavailableException());
    assertThatThrownBy(() -> submit().execute(1L, 10L))
        .isInstanceOf(ApprovalActorUnavailableException.class);
    assertThatThrownBy(() -> approve().execute(1L, 10L))
        .isInstanceOf(ApprovalActorUnavailableException.class);
    assertThatThrownBy(() -> reject().execute(1L, 10L, "valid reason"))
        .isInstanceOf(ApprovalActorUnavailableException.class);
    verifyNoInteractions(accounts, configurations, requests, eligibility, decisions);
  }

  @Test
  void submitWhilePendingIsStrictAndDoesNotMutateWorkflow() {
    when(accounts.findByCompanyIdAndId(1L, 10L))
        .thenReturn(Optional.of(account(FinancialAccountStatus.PENDING_APPROVAL)));
    assertThatThrownBy(() -> submit().execute(1L, 10L))
        .isInstanceOf(InvalidFinancialAccountStatusException.class);
    verify(accounts, never()).updateStatus(any());
    verifyNoInteractions(configurations, requests, decisions);
  }

  @Test
  void rejectWhileDraftIsStrictAndDoesNotMutateWorkflow() {
    when(accounts.findByCompanyIdAndId(1L, 10L))
        .thenReturn(Optional.of(account(FinancialAccountStatus.DRAFT)));
    assertThatThrownBy(() -> reject().execute(1L, 10L, "valid reason"))
        .isInstanceOf(InvalidFinancialAccountStatusException.class);
    verify(accounts, never()).updateStatus(any());
    verifyNoInteractions(requests, eligibility, decisions);
  }

  @Test
  void ineligibleRejectorLeavesCompleteWorkflowUntouched() {
    var account = account(FinancialAccountStatus.PENDING_APPROVAL);
    var request = ApprovalRequest.rehydrate(30L, 10L, "requester", ApprovalRequestStatus.PENDING);
    when(actors.currentActor()).thenReturn(new ApprovalActor("rejector"));
    when(accounts.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(account));
    when(requests.findPendingByFinancialAccountId(10L)).thenReturn(Optional.of(request));
    when(eligibility.canApprove("rejector", 1L)).thenReturn(false);
    assertThatThrownBy(() -> reject().execute(1L, 10L, "valid reason"))
        .isInstanceOf(ApproverNotAllowedException.class);
    assertThat(account.status()).isEqualTo(FinancialAccountStatus.PENDING_APPROVAL);
    assertThat(request.status()).isEqualTo(ApprovalRequestStatus.PENDING);
    verify(accounts, never()).updateStatus(any());
    verify(requests, never()).save(any());
    verifyNoInteractions(decisions);
  }

  @Test
  void branchSpecificConfigurationResultOverridesCompanyWideRule() {
    var account = account(FinancialAccountStatus.DRAFT);
    var branchRule =
        ApprovalConfiguration.rehydrate(2L, 1L, 2L, FinancialAccountType.PAYABLE, true);
    when(accounts.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(account));
    when(configurations.findApplicable(1L, 2L, FinancialAccountType.PAYABLE))
        .thenReturn(Optional.of(branchRule));
    assertThat(submit().execute(1L, 10L).status())
        .isEqualTo(FinancialAccountStatus.PENDING_APPROVAL);
    verify(requests).save(any());
  }

  @Test
  void companyConfigurationResultIsUsedWhenBranchRuleIsAbsent() {
    var account = account(FinancialAccountStatus.DRAFT);
    var companyRule =
        ApprovalConfiguration.rehydrate(1L, 1L, null, FinancialAccountType.PAYABLE, true);
    when(accounts.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(account));
    when(configurations.findApplicable(1L, 2L, FinancialAccountType.PAYABLE))
        .thenReturn(Optional.of(companyRule));
    assertThat(submit().execute(1L, 10L).status())
        .isEqualTo(FinancialAccountStatus.PENDING_APPROVAL);
    verify(requests).save(any());
  }

  @Test
  void strictRepeatedActionsAndCrossCompanyLookupFailWithoutPersistence() {
    when(accounts.findByCompanyIdAndId(2L, 10L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> submit().execute(2L, 10L))
        .isInstanceOf(
            com.financeiro.financialaccount.application.FinancialAccountNotFoundException.class);

    when(accounts.findByCompanyIdAndId(1L, 10L))
        .thenReturn(Optional.of(account(FinancialAccountStatus.APPROVED)));
    assertThatThrownBy(() -> approve().execute(1L, 10L))
        .isInstanceOf(InvalidFinancialAccountStatusException.class);
    verify(accounts, never()).updateStatus(any());
  }

  @Test
  void missingPendingRequestIsAnInternalConsistencyFailure() {
    when(actors.currentActor()).thenReturn(new ApprovalActor("approver"));
    when(accounts.findByCompanyIdAndId(1L, 10L))
        .thenReturn(Optional.of(account(FinancialAccountStatus.PENDING_APPROVAL)));
    when(requests.findPendingByFinancialAccountId(10L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> approve().execute(1L, 10L))
        .isInstanceOf(ApprovalWorkflowConsistencyException.class);
    verifyNoInteractions(eligibility, decisions);
    verify(accounts, never()).updateStatus(any());
  }

  @Test
  void rejectedAccountCanSubmitAgainAndCreatesANewRequestCycle() {
    var account = account(FinancialAccountStatus.PENDING_APPROVAL);
    var oldRequest =
        ApprovalRequest.rehydrate(30L, 10L, "requester", ApprovalRequestStatus.PENDING);
    when(accounts.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(account));
    when(requests.findPendingByFinancialAccountId(10L)).thenReturn(Optional.of(oldRequest));
    when(eligibility.canApprove("requester", 1L)).thenReturn(true);
    when(configurations.findApplicable(1L, 2L, FinancialAccountType.PAYABLE))
        .thenReturn(
            Optional.of(
                ApprovalConfiguration.rehydrate(1L, 1L, 2L, FinancialAccountType.PAYABLE, true)));

    reject().execute(1L, 10L, "retry");
    submit().execute(1L, 10L);

    var cycle = ArgumentCaptor.forClass(ApprovalRequest.class);
    verify(requests, times(2)).save(cycle.capture());
    assertThat(cycle.getAllValues().get(0).status()).isEqualTo(ApprovalRequestStatus.REJECTED);
    assertThat(cycle.getAllValues().get(1).id()).isNull();
    assertThat(cycle.getAllValues().get(1).status()).isEqualTo(ApprovalRequestStatus.PENDING);
  }

  @Test
  void approvalActorIsOpaqueNormalizedAndBounded() {
    assertThat(new ApprovalActor("  external:subject  ").actorId()).isEqualTo("external:subject");
    assertThatThrownBy(() -> new ApprovalActor(null))
        .isInstanceOf(InvalidApprovalActorException.class);
    assertThatThrownBy(() -> new ApprovalActor(" "))
        .isInstanceOf(InvalidApprovalActorException.class);
    assertThatThrownBy(() -> new ApprovalActor("a".repeat(129)))
        .isInstanceOf(InvalidApprovalActorException.class);
  }

  private SubmitFinancialAccountForApproval submit() {
    return new SubmitFinancialAccountForApproval(actors, accounts, configurations, requests);
  }

  private ApproveFinancialAccount approve() {
    return new ApproveFinancialAccount(actors, eligibility, accounts, requests, decisions);
  }

  private RejectFinancialAccount reject() {
    return new RejectFinancialAccount(actors, eligibility, accounts, requests, decisions);
  }

  private static FinancialAccount account(FinancialAccountStatus status) {
    return FinancialAccount.rehydrate(
        10L,
        1L,
        2L,
        FinancialAccountType.PAYABLE,
        3L,
        4L,
        null,
        LocalDate.of(2026, 8, 22),
        BigDecimal.TEN,
        status,
        List.of(Installment.rehydrate(20L, 1, LocalDate.of(2026, 9, 1), BigDecimal.TEN)));
  }
}
