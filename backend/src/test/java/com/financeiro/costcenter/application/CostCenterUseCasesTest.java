package com.financeiro.costcenter.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.financeiro.company.application.*;
import com.financeiro.company.domain.Company;
import com.financeiro.costcenter.domain.CostCenter;
import java.util.*;
import org.junit.jupiter.api.Test;

class CostCenterUseCasesTest {
  CostCenterRepository values = mock(CostCenterRepository.class);
  CompanyRepository companies = mock(CompanyRepository.class);

  @Test
  void exercisesCreateGetListAndDeactivate() {
    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "C")));
    when(values.save(any()))
        .thenAnswer(
            i -> {
              var v = (CostCenter) i.getArgument(0);
              return v.id() == null
                  ? CostCenter.rehydrate(2L, v.companyId(), v.name(), v.active())
                  : v;
            });
    var created = new CreateCostCenter(companies, values).execute(1L, " Ops ");
    assertThat(created.name()).isEqualTo("Ops");
    when(values.findByCompanyIdAndId(1L, 2L)).thenReturn(Optional.of(created));
    assertThat(new GetCostCenter(values).execute(1L, 2L)).isEqualTo(created);
    var q = new PageQuery(0, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC);
    when(values.findPageByCompanyId(1L, q))
        .thenReturn(new PageResult<>(List.of(created), 0, 20, 1, 1));
    assertThat(new ListCostCentersByCompany(companies, values).execute(1L, q).totalElements())
        .isOne();
    assertThat(new DeactivateCostCenter(values).execute(1L, 2L).active()).isFalse();
  }

  @Test
  void scopedMissingAndMissingCompanyAreStable() {
    assertThatThrownBy(() -> new GetCostCenter(values).execute(2L, 2L))
        .isInstanceOf(CostCenterNotFoundException.class);
    var q = new PageQuery(0, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC);
    assertThatThrownBy(() -> new ListCostCentersByCompany(companies, values).execute(9L, q))
        .isInstanceOf(CompanyNotFoundException.class);
  }
}
