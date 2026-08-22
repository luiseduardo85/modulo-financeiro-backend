package com.financeiro.financialaccount.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.financeiro.category.application.*;
import com.financeiro.category.domain.Category;
import com.financeiro.company.application.*;
import com.financeiro.company.domain.*;
import com.financeiro.costcenter.application.*;
import com.financeiro.costcenter.domain.CostCenter;
import com.financeiro.financialaccount.domain.*;
import com.financeiro.history.application.HistoryEntryRepository;
import com.financeiro.partner.application.*;
import com.financeiro.partner.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class FinancialAccountUseCasesTest {
  CompanyRepository companies = mock(CompanyRepository.class);
  BranchRepository branches = mock(BranchRepository.class);
  PartnerRepository partners = mock(PartnerRepository.class);
  CategoryRepository categories = mock(CategoryRepository.class);
  CostCenterRepository costCenters = mock(CostCenterRepository.class);
  FinancialAccountRepository accounts = mock(FinancialAccountRepository.class);
  HistoryEntryRepository history = mock(HistoryEntryRepository.class);
  CreateFinancialAccount create =
      new CreateFinancialAccount(
          companies, branches, partners, categories, costCenters, accounts, history);

  @BeforeEach
  void validReferences() {
    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "Company")));
    when(branches.findByCompanyIdAndId(1L, 2L))
        .thenReturn(Optional.of(Branch.rehydrate(2L, 1L, "Branch")));
    when(categories.findByCompanyIdAndId(1L, 4L))
        .thenReturn(Optional.of(Category.rehydrate(4L, 1L, "Category", true)));
    when(accounts.save(any())).thenAnswer(inv -> persisted(inv.getArgument(0)));
  }

  @Test
  void createsPayableForActiveSupplierWithoutCostCenterAndRecordsCreationHistory() {
    when(partners.findById(3L))
        .thenReturn(Optional.of(partner(Set.of(PartnerRole.SUPPLIER), true)));
    var result = create.execute(command(FinancialAccountType.PAYABLE, null));
    assertThat(result.status()).isEqualTo(FinancialAccountStatus.DRAFT);
    verify(accounts).save(any(FinancialAccount.class));
    verifyNoInteractions(costCenters);

    var entry = ArgumentCaptor.forClass(com.financeiro.history.domain.HistoryEntry.class);
    verify(history).save(entry.capture());
    assertThat(entry.getValue().type())
        .isEqualTo(com.financeiro.history.domain.HistoryEntryType.CREATED);
    assertThat(entry.getValue().financialAccountId()).isEqualTo(result.id());
    assertThat(entry.getValue().actorId()).isNull();
  }

  @Test
  void createsReceivableForBothRolesAndValidCostCenter() {
    when(partners.findById(3L))
        .thenReturn(Optional.of(partner(Set.of(PartnerRole.CUSTOMER, PartnerRole.SUPPLIER), true)));
    when(costCenters.findByCompanyIdAndId(1L, 5L))
        .thenReturn(Optional.of(CostCenter.rehydrate(5L, 1L, "Cost", true)));
    assertThat(create.execute(command(FinancialAccountType.RECEIVABLE, 5L)).costCenterId())
        .isEqualTo(5L);
  }

  @Test
  void createsReceivableForActiveCustomerOnly() {
    when(partners.findById(3L))
        .thenReturn(Optional.of(partner(Set.of(PartnerRole.CUSTOMER), true)));

    var result = create.execute(command(FinancialAccountType.RECEIVABLE, null));

    assertThat(result.type()).isEqualTo(FinancialAccountType.RECEIVABLE);
    verify(accounts).save(any(FinancialAccount.class));
  }

  @Test
  void validatesCandidateBeforeRepositories() {
    var invalid =
        new CreateFinancialAccountCommand(
            1L,
            2L,
            FinancialAccountType.PAYABLE,
            3L,
            4L,
            null,
            LocalDate.now(),
            BigDecimal.TEN,
            List.of());
    assertThatThrownBy(() -> create.execute(invalid))
        .isInstanceOf(InvalidFinancialAccountException.class);
    verifyNoInteractions(companies, branches, partners, categories, costCenters, accounts, history);
  }

  @Test
  void invalidTotalWinsBeforeMissingCompanyLookup() {
    var invalid =
        new CreateFinancialAccountCommand(
            1L,
            2L,
            FinancialAccountType.PAYABLE,
            3L,
            4L,
            null,
            LocalDate.of(2026, 8, 21),
            BigDecimal.ZERO,
            List.of(new CreateInstallmentCommand(1, LocalDate.of(2026, 9, 12), BigDecimal.ONE)));

    assertThatThrownBy(() -> create.execute(invalid))
        .isInstanceOf(InvalidFinancialAccountException.class);
    verifyNoInteractions(companies, branches, partners, categories, costCenters, accounts, history);
  }

  @Test
  void duplicateInstallmentsWinBeforeMissingCompanyLookup() {
    var invalid =
        new CreateFinancialAccountCommand(
            1L,
            2L,
            FinancialAccountType.PAYABLE,
            3L,
            4L,
            null,
            LocalDate.of(2026, 8, 21),
            BigDecimal.TEN,
            List.of(
                new CreateInstallmentCommand(1, LocalDate.of(2026, 9, 12), BigDecimal.ONE),
                new CreateInstallmentCommand(1, LocalDate.of(2026, 10, 12), new BigDecimal("9"))));

    assertThatThrownBy(() -> create.execute(invalid))
        .isInstanceOf(InvalidFinancialAccountException.class);
    verifyNoInteractions(companies, branches, partners, categories, costCenters, accounts, history);
  }

  @Test
  void rejectsMissingAndCrossScopedReferences() {
    when(companies.findById(1L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> create.execute(command(FinancialAccountType.PAYABLE, null)))
        .isInstanceOf(CompanyNotFoundException.class);
    reset(companies);
    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "C")));
    when(branches.findByCompanyIdAndId(1L, 2L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> create.execute(command(FinancialAccountType.PAYABLE, null)))
        .isInstanceOf(BranchNotFoundException.class);
  }

  @Test
  void rejectsPartnerStatesAndRoles() {
    when(partners.findById(3L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> create.execute(command(FinancialAccountType.PAYABLE, null)))
        .isInstanceOf(PartnerNotFoundException.class);
    when(partners.findById(3L))
        .thenReturn(Optional.of(partner(Set.of(PartnerRole.SUPPLIER), false)));
    assertThatThrownBy(() -> create.execute(command(FinancialAccountType.PAYABLE, null)))
        .isInstanceOf(PartnerInactiveException.class);
    when(partners.findById(3L))
        .thenReturn(Optional.of(partner(Set.of(PartnerRole.CUSTOMER), true)));
    assertThatThrownBy(() -> create.execute(command(FinancialAccountType.PAYABLE, null)))
        .isInstanceOf(PartnerRoleNotAllowedException.class);
  }

  @Test
  void rejectsMissingOrInactiveCategoryAndCostCenter() {
    when(partners.findById(3L))
        .thenReturn(Optional.of(partner(Set.of(PartnerRole.SUPPLIER), true)));
    when(categories.findByCompanyIdAndId(1L, 4L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> create.execute(command(FinancialAccountType.PAYABLE, null)))
        .isInstanceOf(CategoryNotFoundException.class);
    when(categories.findByCompanyIdAndId(1L, 4L))
        .thenReturn(Optional.of(Category.rehydrate(4L, 1L, "C", false)));
    assertThatThrownBy(() -> create.execute(command(FinancialAccountType.PAYABLE, null)))
        .isInstanceOf(CategoryInactiveException.class);
    when(categories.findByCompanyIdAndId(1L, 4L))
        .thenReturn(Optional.of(Category.rehydrate(4L, 1L, "C", true)));
    when(costCenters.findByCompanyIdAndId(1L, 5L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> create.execute(command(FinancialAccountType.PAYABLE, 5L)))
        .isInstanceOf(CostCenterNotFoundException.class);
    when(costCenters.findByCompanyIdAndId(1L, 5L))
        .thenReturn(Optional.of(CostCenter.rehydrate(5L, 1L, "CC", false)));
    assertThatThrownBy(() -> create.execute(command(FinancialAccountType.PAYABLE, 5L)))
        .isInstanceOf(CostCenterInactiveException.class);
    verify(accounts, never()).save(any());
    verify(history, never()).save(any());
  }

  @Test
  void rejectsCategoryThatExistsOnlyInAnotherCompany() {
    when(partners.findById(3L))
        .thenReturn(Optional.of(partner(Set.of(PartnerRole.SUPPLIER), true)));
    when(categories.findByCompanyIdAndId(1L, 4L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> create.execute(command(FinancialAccountType.PAYABLE, null)))
        .isInstanceOf(CategoryNotFoundException.class);
    verify(categories).findByCompanyIdAndId(1L, 4L);
    verify(accounts, never()).save(any());
    verify(history, never()).save(any());
  }

  @Test
  void rejectsCostCenterThatExistsOnlyInAnotherCompany() {
    when(partners.findById(3L))
        .thenReturn(Optional.of(partner(Set.of(PartnerRole.SUPPLIER), true)));
    when(costCenters.findByCompanyIdAndId(1L, 5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> create.execute(command(FinancialAccountType.PAYABLE, 5L)))
        .isInstanceOf(CostCenterNotFoundException.class);
    verify(costCenters).findByCompanyIdAndId(1L, 5L);
    verify(accounts, never()).save(any());
    verify(history, never()).save(any());
  }

  @Test
  void savesOnlyAfterEveryReferenceSuccessfullyResolves() {
    when(partners.findById(3L))
        .thenReturn(Optional.of(partner(Set.of(PartnerRole.SUPPLIER), true)));
    when(costCenters.findByCompanyIdAndId(1L, 5L))
        .thenReturn(Optional.of(CostCenter.rehydrate(5L, 1L, "Cost", true)));

    create.execute(command(FinancialAccountType.PAYABLE, 5L));

    InOrder order =
        inOrder(companies, branches, partners, categories, costCenters, accounts, history);
    order.verify(companies).findById(1L);
    order.verify(branches).findByCompanyIdAndId(1L, 2L);
    order.verify(partners).findById(3L);
    order.verify(categories).findByCompanyIdAndId(1L, 4L);
    order.verify(costCenters).findByCompanyIdAndId(1L, 5L);
    order.verify(accounts).save(any(FinancialAccount.class));
    order.verify(history).save(any());
  }

  @Test
  void getsAndListsWithCompanyScope() {
    var persisted = persisted();
    when(accounts.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(persisted));
    assertThat(new GetFinancialAccount(accounts).execute(1L, 10L)).isSameAs(persisted);
    assertThatThrownBy(() -> new GetFinancialAccount(accounts).execute(2L, 10L))
        .isInstanceOf(FinancialAccountNotFoundException.class);
    var query = new FinancialAccountPageQuery(0, 20, FinancialAccountPageQuery.Direction.ASC);
    when(accounts.findSummaryPageByCompanyId(1L, query))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    assertThat(new ListFinancialAccountsByCompany(companies, accounts).execute(1L, query).data())
        .isEmpty();
    when(companies.findById(2L)).thenReturn(Optional.empty());
    assertThatThrownBy(
            () -> new ListFinancialAccountsByCompany(companies, accounts).execute(2L, query))
        .isInstanceOf(CompanyNotFoundException.class);
  }

  @Test
  void listsNonEmptySummaryPageUsingOnlyRequestedCompanyScope() {
    var query = new FinancialAccountPageQuery(1, 10, FinancialAccountPageQuery.Direction.DESC);
    var summary =
        new FinancialAccountSummary(
            10L,
            1L,
            2L,
            FinancialAccountType.PAYABLE,
            3L,
            4L,
            null,
            LocalDate.of(2026, 8, 21),
            BigDecimal.TEN,
            FinancialAccountStatus.DRAFT);
    var expected = new PageResult<>(List.of(summary), 1, 10, 11, 2);
    when(accounts.findSummaryPageByCompanyId(1L, query)).thenReturn(expected);

    var result = new ListFinancialAccountsByCompany(companies, accounts).execute(1L, query);

    assertThat(result).isSameAs(expected);
    assertThat(result.data()).containsExactly(summary);
    assertThat(result.data()).allMatch(item -> item.companyId().equals(1L));
    verify(accounts).findSummaryPageByCompanyId(1L, query);
    verifyNoMoreInteractions(accounts);
  }

  private CreateFinancialAccountCommand command(FinancialAccountType type, Long costCenter) {
    return new CreateFinancialAccountCommand(
        1L,
        2L,
        type,
        3L,
        4L,
        costCenter,
        LocalDate.of(2026, 8, 21),
        new BigDecimal("10.00"),
        List.of(
            new CreateInstallmentCommand(1, LocalDate.of(2026, 9, 12), new BigDecimal("10.00"))));
  }

  private Partner partner(Set<PartnerRole> roles, boolean active) {
    return Partner.rehydrate(3L, "Partner", Document.of("52998224725"), roles, active);
  }

  private static FinancialAccount persisted(FinancialAccount candidate) {
    var installmentId = new long[] {9000L};
    var installments =
        candidate.installments().stream()
            .map(
                item ->
                    Installment.rehydrate(
                        installmentId[0]++,
                        item.installmentNumber(),
                        item.dueDate(),
                        item.amount()))
            .toList();
    return FinancialAccount.rehydrate(
        999L,
        candidate.companyId(),
        candidate.branchId(),
        candidate.type(),
        candidate.partnerId(),
        candidate.categoryId(),
        candidate.costCenterId(),
        candidate.issueDate(),
        candidate.totalAmount(),
        candidate.status(),
        installments);
  }

  private FinancialAccount persisted() {
    return FinancialAccount.rehydrate(
        10L,
        1L,
        2L,
        FinancialAccountType.PAYABLE,
        3L,
        4L,
        null,
        LocalDate.of(2026, 8, 21),
        BigDecimal.TEN,
        FinancialAccountStatus.DRAFT,
        List.of(Installment.rehydrate(11L, 1, LocalDate.of(2026, 9, 12), BigDecimal.TEN)));
  }
}
