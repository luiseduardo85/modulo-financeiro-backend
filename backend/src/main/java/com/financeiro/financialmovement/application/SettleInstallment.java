package com.financeiro.financialmovement.application;

import com.financeiro.bankaccount.application.BankAccountNotFoundException;
import com.financeiro.bankaccount.application.BankAccountRepository;
import com.financeiro.financialaccount.application.FinancialAccountNotFoundException;
import com.financeiro.financialaccount.application.FinancialAccountRepository;
import com.financeiro.financialaccount.domain.FinancialAccount;
import com.financeiro.financialaccount.domain.FinancialAccountStatus;
import com.financeiro.financialaccount.domain.FinancialAccountType;
import com.financeiro.financialaccount.domain.Installment;
import com.financeiro.financialmovement.domain.FinancialMovement;
import com.financeiro.financialmovement.domain.FinancialMovementType;
import com.financeiro.idempotency.application.FinancialIdempotencyService;
import com.financeiro.idempotency.application.IdempotencyClaim;
import com.financeiro.idempotency.application.IdempotencyScope;
import com.financeiro.idempotency.application.Sha256Fingerprint;
import com.financeiro.paymentmethod.application.PaymentMethodNotFoundException;
import com.financeiro.paymentmethod.application.PaymentMethodRepository;
import java.util.List;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettleInstallment {
  static final String OPERATION = "SETTLE_INSTALLMENT";
  private final FinancialIdempotencyService idempotency;
  private final FinancialAccountRepository accounts;
  private final FinancialMovementRepository movements;
  private final SettlementBalanceRepository balances;
  private final BankAccountRepository bankAccounts;
  private final PaymentMethodRepository paymentMethods;

  public SettleInstallment(
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
  public SettlementResult execute(SettleInstallmentCommand command) {
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
    if (account.status() != FinancialAccountStatus.APPROVED)
      throw new FinancialAccountNotSettleableException();

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

    forceSettlementVersionIncrement(command);

    var settled = balances.settledAmountByInstallmentId(command.installmentId());
    var remaining = installment.amount().subtract(settled);
    if (remaining.compareTo(java.math.BigDecimal.ZERO) == 0)
      throw new InstallmentAlreadySettledException();
    if (command.amount().compareTo(remaining) > 0)
      throw new SettlementAmountExceedsBalanceException();

    FinancialMovement movement =
        movements.save(
            FinancialMovement.create(
                command.installmentId(),
                movementType(account.type()),
                command.amount(),
                command.movementDate(),
                command.bankAccountId(),
                command.paymentMethodId()));

    if (!balances.hasOpenInstallments(command.financialAccountId())) {
      account.markAsSettled();
      accounts.updateSettlementStatus(account);
    }

    idempotency.complete(claim, movement.id().toString());
    return result(command.financialAccountId(), movement);
  }

  static String fingerprint(SettleInstallmentCommand command) {
    return Sha256Fingerprint.fromOrderedComponents(
        List.of(
            "settlement:v1",
            OPERATION,
            command.companyId().toString(),
            command.financialAccountId().toString(),
            command.installmentId().toString(),
            command.amount().stripTrailingZeros().toPlainString(),
            command.movementDate().toString(),
            command.bankAccountId().toString(),
            command.paymentMethodId().toString()));
  }

  private SettlementResult replay(SettleInstallmentCommand command, String resultReference) {
    try {
      Long movementId = Long.valueOf(resultReference);
      FinancialMovement movement =
          movements
              .findByScopeAndId(
                  command.companyId(),
                  command.financialAccountId(),
                  command.installmentId(),
                  movementId)
              .orElseThrow(SettlementReplayConsistencyException::new);
      return result(command.financialAccountId(), movement);
    } catch (NumberFormatException exception) {
      throw new SettlementReplayConsistencyException();
    }
  }

  private void forceSettlementVersionIncrement(SettleInstallmentCommand command) {
    try {
      accounts.forceSettlementVersionIncrement(command.companyId(), command.financialAccountId());
    } catch (OptimisticLockingFailureException exception) {
      throw new SettlementConflictException(exception);
    }
  }

  private static FinancialMovementType movementType(FinancialAccountType accountType) {
    return accountType == FinancialAccountType.PAYABLE
        ? FinancialMovementType.PAYMENT
        : FinancialMovementType.RECEIPT;
  }

  private static SettlementResult result(Long financialAccountId, FinancialMovement movement) {
    return new SettlementResult(
        movement.id(),
        financialAccountId,
        movement.installmentId(),
        movement.type(),
        movement.amount(),
        movement.movementDate(),
        movement.bankAccountId(),
        movement.paymentMethodId());
  }
}
