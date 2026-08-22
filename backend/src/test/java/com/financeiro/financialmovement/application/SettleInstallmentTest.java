package com.financeiro.financialmovement.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.financeiro.bankaccount.application.BankAccountNotFoundException;
import com.financeiro.bankaccount.application.BankAccountRepository;
import com.financeiro.bankaccount.domain.BankAccount;
import com.financeiro.financialaccount.application.FinancialAccountNotFoundException;
import com.financeiro.financialaccount.application.FinancialAccountRepository;
import com.financeiro.financialaccount.domain.*;
import com.financeiro.financialmovement.domain.*;
import com.financeiro.idempotency.application.*;
import com.financeiro.paymentmethod.application.PaymentMethodNotFoundException;
import com.financeiro.paymentmethod.application.PaymentMethodRepository;
import com.financeiro.paymentmethod.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;

class SettleInstallmentTest {
  private static final LocalDate DATE = LocalDate.of(2026, 8, 22);
  private final FinancialIdempotencyService idempotency = mock(FinancialIdempotencyService.class);
  private final FinancialAccountRepository accounts = mock(FinancialAccountRepository.class);
  private final FinancialMovementRepository movements = mock(FinancialMovementRepository.class);
  private final SettlementBalanceRepository balances = mock(SettlementBalanceRepository.class);
  private final BankAccountRepository bankAccounts = mock(BankAccountRepository.class);
  private final PaymentMethodRepository paymentMethods = mock(PaymentMethodRepository.class);
  private final SettleInstallment useCase =
      new SettleInstallment(
          idempotency, accounts, movements, balances, bankAccounts, paymentMethods);

  @BeforeEach
  void validReferences() {
    when(idempotency.claim(any(), anyString())).thenReturn(IdempotencyClaim.claimed(90L));
    when(accounts.findByCompanyIdAndId(1L, 10L))
        .thenReturn(
            Optional.of(account(FinancialAccountType.PAYABLE, FinancialAccountStatus.APPROVED)));
    when(bankAccounts.findByCompanyIdAndId(1L, 30L))
        .thenReturn(Optional.of(BankAccount.rehydrate(30L, 1L, null, "Bank", true)));
    when(paymentMethods.findByCompanyIdAndId(1L, 40L))
        .thenReturn(Optional.of(PaymentMethod.rehydrate(40L, 1L, "PIX", true)));
    when(balances.settledAmountByInstallmentId(20L)).thenReturn(BigDecimal.ZERO);
    when(balances.hasOpenInstallments(10L)).thenReturn(true);
    when(movements.save(any())).thenAnswer(invocation -> persisted(invocation.getArgument(0)));
  }

  @Test
  void derivesPaymentAndReceiptFromAccountType() {
    assertThat(useCase.execute(command("40.00")).type()).isEqualTo(FinancialMovementType.PAYMENT);
    reset(movements);
    when(accounts.findByCompanyIdAndId(1L, 10L))
        .thenReturn(
            Optional.of(account(FinancialAccountType.RECEIVABLE, FinancialAccountStatus.APPROVED)));
    when(movements.save(any())).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

    assertThat(useCase.execute(command("40.00")).type()).isEqualTo(FinancialMovementType.RECEIPT);
  }

  @Test
  void partialSettlementForcesVersionAndKeepsAccountApproved() {
    useCase.execute(command("40.00"));

    verify(accounts).forceSettlementVersionIncrement(1L, 10L);
    verify(accounts, never()).updateSettlementStatus(any());
    verify(idempotency).complete(any(), eq("50"));
  }

  @Test
  void finalSettlementMarksAccountSettledOnlyWhenEveryInstallmentIsClosed() {
    when(balances.hasOpenInstallments(10L)).thenReturn(false);
    ArgumentCaptor<FinancialAccount> account = ArgumentCaptor.forClass(FinancialAccount.class);

    useCase.execute(command("100.00"));

    verify(accounts).updateSettlementStatus(account.capture());
    assertThat(account.getValue().status()).isEqualTo(FinancialAccountStatus.SETTLED);
  }

  @ParameterizedTest
  @EnumSource(
      value = FinancialAccountStatus.class,
      names = {"DRAFT", "PENDING_APPROVAL", "SETTLED", "CANCELLED"})
  void rejectsEveryNonApprovedAccountWithoutSettlementMutation(FinancialAccountStatus status) {
    when(accounts.findByCompanyIdAndId(1L, 10L))
        .thenReturn(Optional.of(account(FinancialAccountType.PAYABLE, status)));

    assertThatThrownBy(() -> useCase.execute(command("10.00")))
        .isInstanceOf(FinancialAccountNotSettleableException.class);
    verify(accounts, never()).forceSettlementVersionIncrement(anyLong(), anyLong());
    verify(movements, never()).save(any());
  }

  @Test
  void scopesAccountAndInstallmentWithoutLeakingOtherOwners() {
    when(accounts.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.execute(command("10.00")))
        .isInstanceOf(FinancialAccountNotFoundException.class);

    when(accounts.findByCompanyIdAndId(1L, 10L))
        .thenReturn(
            Optional.of(account(FinancialAccountType.PAYABLE, FinancialAccountStatus.APPROVED)));
    var wrongInstallment = command("10.00", 999L, 30L, 40L, "key-other");
    assertThatThrownBy(() -> useCase.execute(wrongInstallment))
        .isInstanceOf(InstallmentNotFoundException.class);
  }

  @Test
  void rejectsAlreadySettledAndOverpaymentWithoutCreatingMovement() {
    when(balances.settledAmountByInstallmentId(20L)).thenReturn(new BigDecimal("100.00"));
    assertThatThrownBy(() -> useCase.execute(command("1.00")))
        .isInstanceOf(InstallmentAlreadySettledException.class);

    when(balances.settledAmountByInstallmentId(20L)).thenReturn(new BigDecimal("70.00"));
    assertThatThrownBy(() -> useCase.execute(command("40.00")))
        .isInstanceOf(SettlementAmountExceedsBalanceException.class);
    verify(movements, never()).save(any());
  }

  @Test
  void validatesBankAccountCompanyActivityAndBranch() {
    when(bankAccounts.findByCompanyIdAndId(1L, 30L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.execute(command("10.00")))
        .isInstanceOf(BankAccountNotFoundException.class);

    when(bankAccounts.findByCompanyIdAndId(1L, 30L))
        .thenReturn(Optional.of(BankAccount.rehydrate(30L, 1L, null, "Bank", false)));
    assertThatThrownBy(() -> useCase.execute(command("10.00")))
        .isInstanceOf(BankAccountInactiveException.class);

    when(bankAccounts.findByCompanyIdAndId(1L, 30L))
        .thenReturn(Optional.of(BankAccount.rehydrate(30L, 1L, 99L, "Bank", true)));
    assertThatThrownBy(() -> useCase.execute(command("10.00")))
        .isInstanceOf(BankAccountBranchNotAllowedException.class);

    verify(accounts, never()).forceSettlementVersionIncrement(anyLong(), anyLong());
  }

  @Test
  void rejectsBankAccountThatExistsOnlyInAnotherCompany() {
    BankAccount otherCompany = BankAccount.rehydrate(30L, 2L, null, "Other Bank", true);
    assertThat(otherCompany.companyId()).isEqualTo(2L);
    when(bankAccounts.findByCompanyIdAndId(1L, otherCompany.id())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(command("10.00")))
        .isInstanceOf(BankAccountNotFoundException.class);

    verify(bankAccounts).findByCompanyIdAndId(1L, 30L);
    verify(accounts, never()).forceSettlementVersionIncrement(anyLong(), anyLong());
    verify(accounts, never()).updateSettlementStatus(any());
    verify(movements, never()).save(any());
    verify(idempotency, never()).complete(any(), anyString());
  }

  @Test
  void acceptsGlobalAndMatchingBranchBankAccounts() {
    useCase.execute(command("10.00"));
    when(bankAccounts.findByCompanyIdAndId(1L, 30L))
        .thenReturn(Optional.of(BankAccount.rehydrate(30L, 1L, 2L, "Bank", true)));
    useCase.execute(command("10.00", 20L, 30L, 40L, "matching-branch"));
    verify(movements, times(2)).save(any());
  }

  @Test
  void validatesPaymentMethodCompanyAndActivity() {
    when(paymentMethods.findByCompanyIdAndId(1L, 40L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.execute(command("10.00")))
        .isInstanceOf(PaymentMethodNotFoundException.class);

    when(paymentMethods.findByCompanyIdAndId(1L, 40L))
        .thenReturn(Optional.of(PaymentMethod.rehydrate(40L, 1L, "PIX", false)));
    assertThatThrownBy(() -> useCase.execute(command("10.00")))
        .isInstanceOf(PaymentMethodInactiveException.class);
    verify(movements, never()).save(any());
  }

  @Test
  void rejectsPaymentMethodThatExistsOnlyInAnotherCompany() {
    PaymentMethod otherCompany = PaymentMethod.rehydrate(40L, 2L, "Other PIX", true);
    assertThat(otherCompany.companyId()).isEqualTo(2L);
    when(paymentMethods.findByCompanyIdAndId(1L, otherCompany.id())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(command("10.00")))
        .isInstanceOf(PaymentMethodNotFoundException.class);

    verify(paymentMethods).findByCompanyIdAndId(1L, 40L);
    verify(accounts, never()).forceSettlementVersionIncrement(anyLong(), anyLong());
    verify(accounts, never()).updateSettlementStatus(any());
    verify(movements, never()).save(any());
    verify(idempotency, never()).complete(any(), anyString());
  }

  @Test
  void translatesOptimisticVersionConflict() {
    doThrow(new OptimisticLockingFailureException("stale settlement") {})
        .when(accounts)
        .forceSettlementVersionIncrement(1L, 10L);

    assertThatThrownBy(() -> useCase.execute(command("10.00")))
        .isInstanceOf(SettlementConflictException.class)
        .hasCauseInstanceOf(OptimisticLockingFailureException.class);

    verify(movements, never()).save(any());
    verify(idempotency, never()).complete(any(), anyString());
  }

  @Test
  void completedClaimReplaysScopedImmutableMovementWithoutBusinessLookups() {
    when(idempotency.claim(any(), anyString())).thenReturn(IdempotencyClaim.completed(90L, "77"));
    when(movements.findByScopeAndId(1L, 10L, 20L, 77L))
        .thenReturn(
            Optional.of(
                FinancialMovement.rehydrate(
                    77L,
                    20L,
                    FinancialMovementType.PAYMENT,
                    new BigDecimal("40.00"),
                    DATE,
                    30L,
                    40L,
                    null)));

    SettlementResult replay = useCase.execute(command("40.00"));

    assertThat(replay.id()).isEqualTo(77L);
    verifyNoInteractions(bankAccounts, paymentMethods, balances);
    verify(accounts, never()).findByCompanyIdAndId(anyLong(), anyLong());
  }

  @Test
  void fingerprintUsesApprovedOrderAndCanonicalMoney() {
    assertThat(SettleInstallment.fingerprint(command("100")))
        .isEqualTo(SettleInstallment.fingerprint(command("100.00")));
    assertThat(SettleInstallment.fingerprint(command("100.00")))
        .isNotEqualTo(SettleInstallment.fingerprint(command("100.00", 20L, 31L, 40L, "same-key")));
  }

  @Test
  void idempotencyConflictStopsBeforeEveryBusinessRepository() {
    when(idempotency.claim(any(), anyString())).thenThrow(new IdempotencyConflictException());

    assertThatThrownBy(() -> useCase.execute(command("10.00")))
        .isInstanceOf(IdempotencyConflictException.class);

    verifyNoInteractions(accounts, movements, balances, bankAccounts, paymentMethods);
  }

  @Test
  void idempotencyContentionStopsBeforeEveryBusinessRepository() {
    when(idempotency.claim(any(), anyString())).thenThrow(new IdempotencyInProgressException());

    assertThatThrownBy(() -> useCase.execute(command("10.00")))
        .isInstanceOf(IdempotencyInProgressException.class);

    verifyNoInteractions(accounts, movements, balances, bankAccounts, paymentMethods);
  }

  @Test
  void candidateValidationHappensBeforeAnyBoundaryInteraction() {
    assertThatThrownBy(
            () ->
                new SettleInstallmentCommand(
                    1L, 10L, 20L, new BigDecimal("1.001"), DATE, 30L, 40L, "key"))
        .isInstanceOf(InvalidFinancialMovementException.class);
    verifyNoInteractions(idempotency, accounts, movements, balances, bankAccounts, paymentMethods);
  }

  @Test
  void nullMovementDateFailsBeforeEveryBoundary() {
    assertThatThrownBy(
            () -> new SettleInstallmentCommand(1L, 10L, 20L, BigDecimal.ONE, null, 30L, 40L, "key"))
        .isInstanceOf(InvalidFinancialMovementException.class);
    verifyNoInteractions(idempotency, accounts, movements, balances, bankAccounts, paymentMethods);
  }

  @Test
  void invalidBankAccountIdsFailBeforeEveryBoundary() {
    for (Long id : new Long[] {null, 0L, -1L}) {
      assertThatThrownBy(
              () ->
                  new SettleInstallmentCommand(1L, 10L, 20L, BigDecimal.ONE, DATE, id, 40L, "key"))
          .isInstanceOf(InvalidFinancialMovementException.class);
    }
    verifyNoInteractions(idempotency, accounts, movements, balances, bankAccounts, paymentMethods);
  }

  @Test
  void invalidPaymentMethodIdsFailBeforeEveryBoundary() {
    for (Long id : new Long[] {null, 0L, -1L}) {
      assertThatThrownBy(
              () ->
                  new SettleInstallmentCommand(1L, 10L, 20L, BigDecimal.ONE, DATE, 30L, id, "key"))
          .isInstanceOf(InvalidFinancialMovementException.class);
    }
    verifyNoInteractions(idempotency, accounts, movements, balances, bankAccounts, paymentMethods);
  }

  private SettleInstallmentCommand command(String amount) {
    return command(amount, 20L, 30L, 40L, "key-1");
  }

  private SettleInstallmentCommand command(
      String amount, Long installmentId, Long bankAccountId, Long paymentMethodId, String key) {
    return new SettleInstallmentCommand(
        1L, 10L, installmentId, new BigDecimal(amount), DATE, bankAccountId, paymentMethodId, key);
  }

  private static FinancialAccount account(
      FinancialAccountType type, FinancialAccountStatus status) {
    return FinancialAccount.rehydrate(
        10L,
        1L,
        2L,
        type,
        3L,
        4L,
        null,
        DATE,
        new BigDecimal("150.00"),
        status,
        List.of(
            Installment.rehydrate(20L, 1, DATE, new BigDecimal("100.00")),
            Installment.rehydrate(21L, 2, DATE, new BigDecimal("50.00"))));
  }

  private static FinancialMovement persisted(FinancialMovement movement) {
    return FinancialMovement.rehydrate(
        50L,
        movement.installmentId(),
        movement.type(),
        movement.amount(),
        movement.movementDate(),
        movement.bankAccountId(),
        movement.paymentMethodId(),
        movement.originalMovementId());
  }
}
