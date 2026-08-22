package com.financeiro.category.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.financeiro.category.domain.Category;
import com.financeiro.category.domain.InvalidCategoryNameException;
import com.financeiro.company.application.*;
import com.financeiro.company.domain.Company;
import java.util.*;
import org.junit.jupiter.api.Test;

class CategoryUseCasesTest {
  CategoryRepository categories = mock(CategoryRepository.class);
  CompanyRepository companies = mock(CompanyRepository.class);

  @Test
  void createValidatesCompanyAndPersists() {
    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "C")));
    when(categories.save(any()))
        .thenAnswer(i -> Category.rehydrate(2L, 1L, i.<Category>getArgument(0).name(), true));
    assertThat(new CreateCategory(companies, categories).execute(1L, " X ").name()).isEqualTo("X");
  }

  @Test
  void createRequiresCompanyAndValidName() {
    assertThatThrownBy(() -> new CreateCategory(companies, categories).execute(9L, "X"))
        .isInstanceOf(CompanyNotFoundException.class);
    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "C")));
    assertThatThrownBy(() -> new CreateCategory(companies, categories).execute(1L, " "))
        .isInstanceOf(InvalidCategoryNameException.class);
  }

  @Test
  void getIsScopedAndMissingIsNotFound() {
    when(categories.findByCompanyIdAndId(1L, 2L))
        .thenReturn(Optional.of(Category.rehydrate(2L, 1L, "X", false)));
    assertThat(new GetCategory(categories).execute(1L, 2L).active()).isFalse();
    assertThatThrownBy(() -> new GetCategory(categories).execute(2L, 2L))
        .isInstanceOf(CategoryNotFoundException.class);
  }

  @Test
  void listChecksCompanyAndIncludesRepositoryPage() {
    var q = new PageQuery(0, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC);
    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "C")));
    when(categories.findPageByCompanyId(1L, q))
        .thenReturn(new PageResult<>(List.of(Category.rehydrate(2L, 1L, "X", false)), 0, 20, 1, 1));
    assertThat(new ListCategoriesByCompany(companies, categories).execute(1L, q).data()).hasSize(1);
    assertThatThrownBy(() -> new ListCategoriesByCompany(companies, categories).execute(9L, q))
        .isInstanceOf(CompanyNotFoundException.class);
  }

  @Test
  void deactivateIsScopedAndRepeatable() {
    var value = Category.rehydrate(2L, 1L, "X", true);
    when(categories.findByCompanyIdAndId(1L, 2L)).thenReturn(Optional.of(value));
    when(categories.save(any())).thenAnswer(i -> i.getArgument(0));
    var useCase = new DeactivateCategory(categories);
    assertThat(useCase.execute(1L, 2L).active()).isFalse();
    assertThat(useCase.execute(1L, 2L).active()).isFalse();
  }
}
