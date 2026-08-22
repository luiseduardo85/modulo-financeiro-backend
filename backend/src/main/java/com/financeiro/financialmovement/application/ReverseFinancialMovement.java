package com.financeiro.financialmovement.application;

import com.financeiro.bankaccount.application.BankAccountNotFoundException;
import com.financeiro.bankaccount.application.BankAccountRepository;
import com.financeiro.financialaccount.application.FinancialAccountNotFoundException;
import com.financeiro.financialaccount.application.FinancialAccountRepository;
import com.financeiro.financialaccount.domain.FinancialAccount;
import com.financeiro.financialaccount.domain.FinancialAccountStatus;
import com.financeiro.financialaccount.domain.Installment;
import com.financeiro.financialmovement.domain.FinancialMovement;
import com.financeiro.idempotency.application.FinancialIdempotencyService;
import com.financeiro.idempotency.application.IdempotencyClaim;
import com.financeiro.idempotency.application.IdempotencyScope;
import com.financeiro.idempotency.application.Sha256Fingerprint;
import com.financeiro.paymentmethod.application.PaymentMethodNotFoundException;
import com.financeiro.paymentmethod.application.PaymentMethodRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReverseFinancialMovement {
  static final String OPERATION = "REVERSE_FINANCIAL_MOVEMENT";
  private final FinancialIdempotencyService idempotency;
  private final FinancialAccountRepository accounts;
  private final FinancialMovementRepository movements;
  private final SettlementBalanceRepository balances;
  private final BankAccountRepository bankAccounts;
  private final PaymentMethodRepository paymentMethods;

  public ReverseFinancialMovement(
      FinancialIdempotencyService idempotency,
      FinancialAccountRepository accounts,
      FinancialMovementRepository movements,
      SettlementBalanceRepository balances,
      BankAccountRepository bankAccounts,
      PaymentMethodRepository paymentMethods) {
    this.idempotency = idempotency;
    this.accounts = accounts;
    this.movements = movements;
    this.balances = balances;
    this.bankAccounts = bankAccounts;
    this.paymentMethods = paymentMethods;
  }

  @Transactional
  public ReversalResult execute(ReverseFinancialMovementCommand command) {
    String fingerprint = fingerprint(command);
    var scope = new IdempotencyScope(command.companyId(), OPERATION, command.idempotencyKey());
    IdempotencyClaim claim = idempotency.claim(scope, fingerprint);
    if (claim.outcome() == IdempotencyClaim.Outcome.COMPLETED) {
      return replay(command, claim.resultReference());
    }

    FinancialAccount account =
        accounts
            .findByCompanyIdAndId(command.companyId(), command.financialAccountId())
            .orElseThrow(() -> new FinancialAccountNotFoundException(command.financialAccountId()));
    Installment installment =
        account.installments().stream()
            .filter(candidate -> candidate.id().equals(command.installmentId()))
            .findFirst()
            .orElseThrow(() -> new InstallmentNotFoundException(command.installmentId()));
    if (account.status() != FinancialAccountStatus.APPROVED
        && account.status() != FinancialAccountStatus.SETTLED)
      throw new FinancialAccountNotReversibleException();

    FinancialMovement original =
        movements
            .findByScopeAndId(
                command.companyId(),
                command.financialAccountId(),
                command.installmentId(),
                command.movementId())
            .orElseThrow(() -> new OriginalMovementNotFoundException(command.movementId()));
    if (original.type().isReversal()) throw new CannotReverseReversalException();

    var bankAccount =
        bankAccounts
            .findByCompanyIdAndId(command.companyId(), command.bankAccountId())
            .orElseThrow(
                () ->
                    new BankAccountNotFoundException(command.companyId(), command.bankAccountId()));
    if (!bankAccount.active()) throw new BankAccountInactiveException();
    if (bankAccount.branchId() != null && !bankAccount.branchId().equals(account.branchId()))
      throw new BankAccountBranchNotAllowedException();

    var paymentMethod =
        paymentMethods
            .findByCompanyIdAndId(command.companyId(), command.paymentMethodId())
            .orElseThrow(
                () ->
                    new PaymentMethodNotFoundException(
                        command.companyId(), command.paymentMethodId()));
    if (!paymentMethod.active()) throw new PaymentMethodInactiveException();

    forceVersionIncrement(command);

    var reversedSoFar = balances.reversedAmountByOriginalMovementId(command.movementId());
    var remaining = original.amount().subtract(reversedSoFar);
    if (remaining.compareTo(BigDecimal.ZERO) == 0)
      throw new OriginalMovementAlreadyFullyReversedException();
    if (command.amount().compareTo(remaining) > 0)
      throw new ReversalAmountExceedsBalanceException();

    FinancialMovement reversal =
        movements.save(
            FinancialMovement.createReversal(
                command.installmentId(),
                original.type().reversalType(),
                command.amount(),
                command.movementDate(),
                command.bankAccountId(),
                command.paymentMethodId(),
                command.movementId()));

    if (account.status() == FinancialAccountStatus.SETTLED) {
      account.reopen();
      accounts.updateSettlementStatus(account);
    }

    idempotency.complete(claim, reversal.id().toString());
    return result(command.financialAccountId(), reversal);
  }

  static String fingerprint(ReverseFinancialMovementCommand command) {
    return Sha256Fingerprint.fromOrderedComponents(
        List.of(
            "reversal:v1",
            OPERATION,
            command.companyId().toString(),
            command.financialAccountId().toString(),
            command.installmentId().toString(),
            command.movementId().toString(),
            command.amount().stripTrailingZeros().toPlainString(),
            command.movementDate().toString(),
            command.bankAccountId().toString(),
            command.paymentMethodId().toString()));
  }

  private ReversalResult replay(ReverseFinancialMovementCommand command, String resultReference) {
    try {
      Long reversalId = Long.valueOf(resultReference);
      FinancialMovement reversal =
          movements
              .findByScopeAndId(
                  command.companyId(),
                  command.financialAccountId(),
                  command.installmentId(),
                  reversalId)
              .orElseThrow(SettlementReplayConsistencyException::new);
      return result(command.financialAccountId(), reversal);
    } catch (NumberFormatException exception) {
      throw new SettlementReplayConsistencyException();
    }
  }

  private void forceVersionIncrement(ReverseFinancialMovementCommand command) {
    try {
      accounts.forceSettlementVersionIncrement(command.companyId(), command.financialAccountId());
    } catch (OptimisticLockingFailureException exception) {
      throw new SettlementConflictException(exception);
    }
  }

  private static ReversalResult result(Long financialAccountId, FinancialMovement reversal) {
    return new ReversalResult(
        reversal.id(),
        financialAccountId,
        reversal.installmentId(),
        reversal.originalMovementId(),
        reversal.type(),
        reversal.amount(),
        reversal.movementDate(),
        reversal.bankAccountId(),
        reversal.paymentMethodId());
  }
}
