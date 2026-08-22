package com.financeiro.bankaccount.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.financeiro.bankaccount.domain.BankAccount;
import com.financeiro.bankaccount.domain.InvalidBankAccountNameException;
import com.financeiro.company.application.*;
import com.financeiro.company.domain.Branch;
import com.financeiro.company.domain.Company;
import java.util.*;
import org.junit.jupiter.api.Test;

class BankAccountUseCasesTest {
  BankAccountRepository accounts = mock(BankAccountRepository.class);
  CompanyRepository companies = mock(CompanyRepository.class);
  BranchRepository branches = mock(BranchRepository.class);

  @Test
  void createsCompanyWideAndSameCompanyRestrictedAccounts() {
    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "C")));
    when(accounts.save(any())).thenAnswer(i -> i.getArgument(0));
    var useCase = new CreateBankAccount(companies, branches, accounts);
    assertThat(useCase.execute(1L, null, " General ").branchId()).isNull();
    verifyNoInteractions(branches);

    when(branches.findByCompanyIdAndId(1L, 2L))
        .thenReturn(Optional.of(Branch.rehydrate(2L, 1L, "B")));
    assertThat(useCase.execute(1L, 2L, "Restricted").branchId()).isEqualTo(2L);
  }

  @Test
  void rejectsMissingCompanyAndNonexistentOrCrossCompanyBranchWithoutLeakage() {
    var useCase = new CreateBankAccount(companies, branches, accounts);
    assertThatThrownBy(() -> useCase.execute(9L, null, "X"))
        .isInstanceOf(CompanyNotFoundException.class);
    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "C")));
    assertThatThrownBy(() -> useCase.execute(1L, 99L, "X"))
        .isInstanceOf(BranchNotFoundException.class);
    when(branches.findByCompanyIdAndId(1L, 2L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.execute(1L, 2L, "X"))
        .isInstanceOf(BranchNotFoundException.class);
  }

  @Test
  void invalidCandidateIsRejectedBeforeCompanyLookup() {
    var useCase = new CreateBankAccount(companies, branches, accounts);

    assertThatThrownBy(() -> useCase.execute(9L, null, " "))
        .isInstanceOf(InvalidBankAccountNameException.class);
    verifyNoInteractions(companies, branches, accounts);
  }

  @Test
  void listHandlesMissingCompanyAndExistingCompanyWithoutRows() {
    var query = new PageQuery(0, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC);
    var useCase = new ListBankAccountsByCompany(companies, accounts);
    assertThatThrownBy(() -> useCase.execute(9L, query))
        .isInstanceOf(CompanyNotFoundException.class);
    verify(accounts, never()).findPageByCompanyId(anyLong(), any());

    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "C")));
    when(accounts.findPageByCompanyId(1L, query))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    assertThat(useCase.execute(1L, query).data()).isEmpty();
    verify(accounts).findPageByCompanyId(1L, query);
  }

  @Test
  void deactivateMissingOrCrossCompanyResourceIsNotFound() {
    var useCase = new DeactivateBankAccount(accounts);
    assertThatThrownBy(() -> useCase.execute(1L, 99L))
        .isInstanceOf(BankAccountNotFoundException.class);
    assertThatThrownBy(() -> useCase.execute(2L, 3L))
        .isInstanceOf(BankAccountNotFoundException.class);
    verify(accounts).findByCompanyIdAndId(1L, 99L);
    verify(accounts).findByCompanyIdAndId(2L, 3L);
    verify(accounts, never()).save(any());
  }

  @Test
  void getListAndDeactivateRemainCompanyScopedAndIncludeInactive() {
    var inactive = BankAccount.rehydrate(2L, 1L, null, "X", false);
    when(accounts.findByCompanyIdAndId(1L, 2L)).thenReturn(Optional.of(inactive));
    assertThat(new GetBankAccount(accounts).execute(1L, 2L).active()).isFalse();
    assertThatThrownBy(() -> new GetBankAccount(accounts).execute(9L, 2L))
        .isInstanceOf(BankAccountNotFoundException.class);

    var query = new PageQuery(0, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC);
    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "C")));
    when(accounts.findPageByCompanyId(1L, query))
        .thenReturn(new PageResult<>(List.of(inactive), 0, 20, 1, 1));
    assertThat(new ListBankAccountsByCompany(companies, accounts).execute(1L, query).data())
        .containsExactly(inactive);

    var active = BankAccount.rehydrate(3L, 1L, null, "Y", true);
    when(accounts.findByCompanyIdAndId(1L, 3L)).thenReturn(Optional.of(active));
    when(accounts.save(any())).thenAnswer(i -> i.getArgument(0));
    var deactivate = new DeactivateBankAccount(accounts);
    assertThat(deactivate.execute(1L, 3L).active()).isFalse();
    assertThat(deactivate.execute(1L, 3L).active()).isFalse();
  }
}
