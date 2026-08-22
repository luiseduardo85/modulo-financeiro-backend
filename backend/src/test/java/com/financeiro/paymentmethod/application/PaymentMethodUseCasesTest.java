package com.financeiro.paymentmethod.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.financeiro.company.application.*;
import com.financeiro.company.domain.Company;
import com.financeiro.paymentmethod.domain.InvalidPaymentMethodNameException;
import com.financeiro.paymentmethod.domain.PaymentMethod;
import java.util.*;
import org.junit.jupiter.api.Test;

class PaymentMethodUseCasesTest {
  PaymentMethodRepository methods = mock(PaymentMethodRepository.class);
  CompanyRepository companies = mock(CompanyRepository.class);

  @Test
  void createRequiresCompanyAndPersistsNormalizedMethod() {
    assertThatThrownBy(() -> new CreatePaymentMethod(companies, methods).execute(9L, "PIX"))
        .isInstanceOf(CompanyNotFoundException.class);
    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "C")));
    when(methods.save(any())).thenAnswer(i -> i.getArgument(0));
    assertThat(new CreatePaymentMethod(companies, methods).execute(1L, " PIX ").name())
        .isEqualTo("PIX");
  }

  @Test
  void invalidCandidateIsRejectedBeforeCompanyLookup() {
    assertThatThrownBy(() -> new CreatePaymentMethod(companies, methods).execute(9L, " "))
        .isInstanceOf(InvalidPaymentMethodNameException.class);
    verifyNoInteractions(companies, methods);
  }

  @Test
  void listHandlesMissingCompanyAndExistingCompanyWithoutRows() {
    var query = new PageQuery(0, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC);
    var useCase = new ListPaymentMethodsByCompany(companies, methods);
    assertThatThrownBy(() -> useCase.execute(9L, query))
        .isInstanceOf(CompanyNotFoundException.class);
    verify(methods, never()).findPageByCompanyId(anyLong(), any());

    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "C")));
    when(methods.findPageByCompanyId(1L, query))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    assertThat(useCase.execute(1L, query).data()).isEmpty();
    verify(methods).findPageByCompanyId(1L, query);
  }

  @Test
  void deactivateMissingOrCrossCompanyResourceIsNotFound() {
    var useCase = new DeactivatePaymentMethod(methods);
    assertThatThrownBy(() -> useCase.execute(1L, 99L))
        .isInstanceOf(PaymentMethodNotFoundException.class);
    assertThatThrownBy(() -> useCase.execute(2L, 3L))
        .isInstanceOf(PaymentMethodNotFoundException.class);
    verify(methods).findByCompanyIdAndId(1L, 99L);
    verify(methods).findByCompanyIdAndId(2L, 3L);
    verify(methods, never()).save(any());
  }

  @Test
  void getListAndDeactivateRemainCompanyScopedAndIncludeInactive() {
    var inactive = PaymentMethod.rehydrate(2L, 1L, "Cash", false);
    when(methods.findByCompanyIdAndId(1L, 2L)).thenReturn(Optional.of(inactive));
    assertThat(new GetPaymentMethod(methods).execute(1L, 2L).active()).isFalse();
    assertThatThrownBy(() -> new GetPaymentMethod(methods).execute(9L, 2L))
        .isInstanceOf(PaymentMethodNotFoundException.class);

    var query = new PageQuery(0, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC);
    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "C")));
    when(methods.findPageByCompanyId(1L, query))
        .thenReturn(new PageResult<>(List.of(inactive), 0, 20, 1, 1));
    assertThat(new ListPaymentMethodsByCompany(companies, methods).execute(1L, query).data())
        .containsExactly(inactive);

    var active = PaymentMethod.rehydrate(3L, 1L, "PIX", true);
    when(methods.findByCompanyIdAndId(1L, 3L)).thenReturn(Optional.of(active));
    when(methods.save(any())).thenAnswer(i -> i.getArgument(0));
    var deactivate = new DeactivatePaymentMethod(methods);
    assertThat(deactivate.execute(1L, 3L).active()).isFalse();
    assertThat(deactivate.execute(1L, 3L).active()).isFalse();
  }
}
