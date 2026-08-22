package com.financeiro.approval.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.financeiro.approval.domain.ApprovalConfiguration;
import com.financeiro.company.application.*;
import com.financeiro.company.domain.*;
import com.financeiro.financialaccount.domain.FinancialAccountType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SaveApprovalConfigurationTest {
  @Test
  void validatesBranchWithCompanyScopedLookupBeforeSaving() {
    var companies = mock(CompanyRepository.class);
    var branches = mock(BranchRepository.class);
    var configurations = mock(ApprovalConfigurationRepository.class);
    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "Company")));
    when(branches.findByCompanyIdAndId(1L, 2L))
        .thenReturn(Optional.of(Branch.rehydrate(2L, 1L, "Branch")));
    when(configurations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    new SaveApprovalConfiguration(companies, branches, configurations)
        .execute(1L, 2L, FinancialAccountType.PAYABLE, true);

    verify(branches).findByCompanyIdAndId(1L, 2L);
    verify(configurations).save(any(ApprovalConfiguration.class));
  }

  @Test
  void crossCompanyBranchIsNotFoundAndNeverSaved() {
    var companies = mock(CompanyRepository.class);
    var branches = mock(BranchRepository.class);
    var configurations = mock(ApprovalConfigurationRepository.class);
    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "Company A")));
    when(branches.findByCompanyIdAndId(1L, 20L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                new SaveApprovalConfiguration(companies, branches, configurations)
                    .execute(1L, 20L, FinancialAccountType.PAYABLE, true))
        .isInstanceOf(BranchNotFoundException.class);
    verify(configurations, never()).save(any());
  }
}
