package com.financeiro.financialaccount.domain;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinancialAccountTest {

  @Test
  void approvedAccountCanBeMarkedSettledAndAllOtherStatusesCannot() {
    var approved = rehydrate(FinancialAccountStatus.APPROVED);
    approved.markAsSettled();
    assertThat(approved.status()).isEqualTo(FinancialAccountStatus.SETTLED);

    for (FinancialAccountStatus status :
        List.of(
            FinancialAccountStatus.DRAFT,
            FinancialAccountStatus.PENDING_APPROVAL,
            FinancialAccountStatus.SETTLED,
            FinancialAccountStatus.CANCELLED)) {
      assertThatThrownBy(() -> rehydrate(status).markAsSettled())
          .isInstanceOf(InvalidFinancialAccountStatusException.class);
    }
  }

  @Test
  void settledAccountCanBeReopenedAndAllOtherStatusesCannot() {
    var settled = rehydrate(FinancialAccountStatus.SETTLED);
    settled.reopen();
    assertThat(settled.status()).isEqualTo(FinancialAccountStatus.APPROVED);

    for (FinancialAccountStatus status :
        List.of(
            FinancialAccountStatus.DRAFT,
            FinancialAccountStatus.PENDING_APPROVAL,
            FinancialAccountStatus.APPROVED,
            FinancialAccountStatus.CANCELLED)) {
      assertThatThrownBy(() -> rehydrate(status).reopen())
          .isInstanceOf(InvalidFinancialAccountStatusException.class);
    }
  }

  private static final LocalDate DATE = LocalDate.of(2026, 8, 21);

  @Test
  void createsDraftWithNoncontiguousInstallmentsAndDefensiveCollection() {
    var source = new ArrayList<>(List.of(item(1, "100.00"), item(3, "50.000")));
    var account =
        FinancialAccount.create(
            1L,
            2L,
            FinancialAccountType.PAYABLE,
            3L,
            4L,
            null,
            DATE,
            new BigDecimal("150.0"),
            source);
    source.clear();
    assertThat(account.status()).isEqualTo(FinancialAccountStatus.DRAFT);
    assertThat(account.costCenterId()).isNull();
    assertThat(account.installments()).hasSize(2);
    assertThatThrownBy(() -> account.installments().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void acceptsReceivableWeekendAndPositiveCostCenter() {
    var saturday = LocalDate.of(2026, 9, 12);
    var account =
        FinancialAccount.create(
            1L,
            2L,
            FinancialAccountType.RECEIVABLE,
            3L,
            4L,
            5L,
            DATE,
            new BigDecimal("100.50"),
            List.of(Installment.create(8, saturday, new BigDecimal("100.50"))));
    assertThat(account.costCenterId()).isEqualTo(5L);
    assertThat(account.installments().getFirst().dueDate()).isEqualTo(saturday);
  }

  @Test
  void rejectsInvalidStructureAndMoney() {
    assertThatThrownBy(() -> create(0L, 2L, 3L, 4L, null, DATE, "10.00", List.of(item(1, "10.00"))))
        .isInstanceOf(InvalidFinancialAccountException.class);
    assertThatThrownBy(() -> create(1L, 0L, 3L, 4L, null, DATE, "10.00", List.of(item(1, "10.00"))))
        .isInstanceOf(InvalidFinancialAccountException.class);
    assertThatThrownBy(() -> create(1L, 2L, 0L, 4L, null, DATE, "10.00", List.of(item(1, "10.00"))))
        .isInstanceOf(InvalidFinancialAccountException.class);
    assertThatThrownBy(() -> create(1L, 2L, 3L, 0L, null, DATE, "10.00", List.of(item(1, "10.00"))))
        .isInstanceOf(InvalidFinancialAccountException.class);
    assertThatThrownBy(() -> create(1L, 2L, 3L, 4L, 0L, DATE, "10.00", List.of(item(1, "10.00"))))
        .isInstanceOf(InvalidFinancialAccountException.class);
    assertThatThrownBy(
            () ->
                FinancialAccount.create(
                    1L, 2L, null, 3L, 4L, null, DATE, BigDecimal.TEN, List.of(item(1, "10"))))
        .isInstanceOf(InvalidFinancialAccountException.class);
    assertThatThrownBy(() -> create(1L, 2L, 3L, 4L, null, null, "10", List.of(item(1, "10"))))
        .isInstanceOf(InvalidFinancialAccountException.class);
    assertThatThrownBy(
            () -> create(1L, 2L, 3L, 4L, null, DATE, "100.501", List.of(item(1, "100.50"))))
        .isInstanceOf(InvalidFinancialAccountException.class);
    assertThatThrownBy(() -> create(1L, 2L, 3L, 4L, null, DATE, "10", List.of()))
        .isInstanceOf(InvalidFinancialAccountException.class);
    assertThatThrownBy(
            () -> create(1L, 2L, 3L, 4L, null, DATE, "10", List.of(item(1, "5"), item(1, "5"))))
        .isInstanceOf(InvalidFinancialAccountException.class);
    assertThatThrownBy(() -> create(1L, 2L, 3L, 4L, null, DATE, "10", List.of(item(1, "9.99"))))
        .isInstanceOf(InvalidFinancialAccountException.class);
  }

  @Test
  void rejectsNullInstallmentListAndNullMember() {
    assertThatThrownBy(() -> create(1L, 2L, 3L, 4L, null, DATE, "10", null))
        .isInstanceOf(InvalidFinancialAccountException.class);
    var withNull = new ArrayList<Installment>();
    withNull.add(null);
    assertThatThrownBy(() -> create(1L, 2L, 3L, 4L, null, DATE, "10", withNull))
        .isInstanceOf(InvalidFinancialAccountException.class);
  }

  @Test
  void rejectsNullTotalAmount() {
    assertThatThrownBy(
            () ->
                FinancialAccount.create(
                    1L,
                    2L,
                    FinancialAccountType.PAYABLE,
                    3L,
                    4L,
                    null,
                    DATE,
                    null,
                    List.of(item(1, "10"))))
        .isInstanceOf(InvalidFinancialAccountException.class);
  }

  @Test
  void rejectsNonPositiveRehydrationId() {
    for (long id : List.of(0L, -1L)) {
      assertThatThrownBy(
              () ->
                  FinancialAccount.rehydrate(
                      id,
                      1L,
                      2L,
                      FinancialAccountType.PAYABLE,
                      3L,
                      4L,
                      null,
                      DATE,
                      BigDecimal.TEN,
                      FinancialAccountStatus.DRAFT,
                      List.of(Installment.rehydrate(20L, 1, DATE, BigDecimal.TEN))))
          .isInstanceOf(InvalidFinancialAccountException.class);
    }
  }

  @Test
  void rehydratesEveryApprovedStatusWithPersistentIds() {
    for (var status : FinancialAccountStatus.values()) {
      var value =
          FinancialAccount.rehydrate(
              10L,
              1L,
              2L,
              FinancialAccountType.PAYABLE,
              3L,
              4L,
              null,
              DATE,
              BigDecimal.TEN,
              status,
              List.of(Installment.rehydrate(20L, 1, DATE, BigDecimal.TEN)));
      assertThat(value.status()).isEqualTo(status);
    }
  }

  @Test
  void performsOnlyApprovedApprovalTransitions() {
    var submitted = rehydrate(FinancialAccountStatus.DRAFT);
    submitted.submitForApproval();
    assertThat(submitted.status()).isEqualTo(FinancialAccountStatus.PENDING_APPROVAL);

    var direct = rehydrate(FinancialAccountStatus.DRAFT);
    direct.approveWithoutWorkflow();
    assertThat(direct.status()).isEqualTo(FinancialAccountStatus.APPROVED);

    var approved = rehydrate(FinancialAccountStatus.PENDING_APPROVAL);
    approved.approve();
    assertThat(approved.status()).isEqualTo(FinancialAccountStatus.APPROVED);

    var rejected = rehydrate(FinancialAccountStatus.PENDING_APPROVAL);
    rejected.reject();
    assertThat(rejected.status()).isEqualTo(FinancialAccountStatus.DRAFT);
  }

  @Test
  void rejectsEveryApprovalTransitionFromIncompatibleStatuses() {
    for (var status : FinancialAccountStatus.values()) {
      if (status != FinancialAccountStatus.DRAFT) {
        assertThatThrownBy(() -> rehydrate(status).submitForApproval())
            .isInstanceOf(InvalidFinancialAccountStatusException.class);
        assertThatThrownBy(() -> rehydrate(status).approveWithoutWorkflow())
            .isInstanceOf(InvalidFinancialAccountStatusException.class);
      }
      if (status != FinancialAccountStatus.PENDING_APPROVAL) {
        assertThatThrownBy(() -> rehydrate(status).approve())
            .isInstanceOf(InvalidFinancialAccountStatusException.class);
        assertThatThrownBy(() -> rehydrate(status).reject())
            .isInstanceOf(InvalidFinancialAccountStatusException.class);
      }
    }
  }

  private static FinancialAccount rehydrate(FinancialAccountStatus status) {
    return FinancialAccount.rehydrate(
        10L,
        1L,
        2L,
        FinancialAccountType.PAYABLE,
        3L,
        4L,
        null,
        DATE,
        BigDecimal.TEN,
        status,
        List.of(Installment.rehydrate(20L, 1, DATE, BigDecimal.TEN)));
  }

  private static FinancialAccount create(
      Long company,
      Long branch,
      Long partner,
      Long category,
      Long costCenter,
      LocalDate issueDate,
      String total,
      List<Installment> installments) {
    return FinancialAccount.create(
        company,
        branch,
        FinancialAccountType.PAYABLE,
        partner,
        category,
        costCenter,
        issueDate,
        new BigDecimal(total),
        installments);
  }

  private static Installment item(int number, String amount) {
    return Installment.create(number, DATE, new BigDecimal(amount));
  }
}
