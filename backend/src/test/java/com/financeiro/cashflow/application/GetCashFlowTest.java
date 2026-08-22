package com.financeiro.cashflow.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.financeiro.cashflow.domain.CashFlowStatus;
import com.financeiro.company.application.BranchNotFoundException;
import com.financeiro.company.application.BranchRepository;
import com.financeiro.company.application.CompanyNotFoundException;
import com.financeiro.company.application.CompanyRepository;
import com.financeiro.company.domain.Company;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetCashFlowTest {
  private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
  private static final LocalDate START = LocalDate.of(2026, 8, 1);
  private static final LocalDate END = LocalDate.of(2026, 8, 31);

  private final CompanyRepository companies = mock(CompanyRepository.class);
  private final BranchRepository branches = mock(BranchRepository.class);
  private final CashFlowRepository repository = mock(CashFlowRepository.class);
  private final Clock clock =
      Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
  private final GetCashFlow useCase = new GetCashFlow(companies, branches, repository, clock);

  @BeforeEach
  void validCompany() {
    when(companies.findById(1L)).thenReturn(Optional.of(Company.rehydrate(1L, "Acme")));
  }

  @Test
  void rejectsUnknownCompanyWithoutTouchingCashFlowRepository() {
    when(companies.findById(2L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(new CashFlowQuery(2L, null, START, END)))
        .isInstanceOf(CompanyNotFoundException.class);

    verifyNoInteractions(repository);
  }

  @Test
  void rejectsUnknownBranchScopedToCompanyWithoutTouchingCashFlowRepository() {
    when(branches.findByCompanyIdAndId(1L, 9L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(new CashFlowQuery(1L, 9L, START, END)))
        .isInstanceOf(BranchNotFoundException.class);

    verifyNoInteractions(repository);
  }

  @Test
  void skipsBranchLookupWhenBranchIdAbsent() {
    when(repository.forecastByCompany(any())).thenReturn(List.of());
    when(repository.actualByCompany(any())).thenReturn(List.of());

    useCase.execute(new CashFlowQuery(1L, null, START, END));

    verifyNoInteractions(branches);
  }

  @Test
  void returnsEmptyProjectionWithZeroSummaryWhenNothingMatches() {
    when(repository.forecastByCompany(any())).thenReturn(List.of());
    when(repository.actualByCompany(any())).thenReturn(List.of());

    var projection = useCase.execute(new CashFlowQuery(1L, null, START, END));

    assertThat(projection.days()).isEmpty();
    assertThat(projection.summary().totalForecastInflow()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(projection.summary().totalForecastOutflow()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(projection.summary().totalActualInflow()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(projection.summary().totalActualOutflow()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void marksPastForecastDatesOverdueAndFutureDatesScheduled() {
    LocalDate past = TODAY.minusDays(1);
    LocalDate future = TODAY.plusDays(1);
    when(repository.forecastByCompany(any()))
        .thenReturn(
            List.of(
                new CashFlowAggregateRow(past, BigDecimal.ZERO, new BigDecimal("100.00")),
                new CashFlowAggregateRow(future, new BigDecimal("50.00"), BigDecimal.ZERO)));
    when(repository.actualByCompany(any())).thenReturn(List.of());

    var projection = useCase.execute(new CashFlowQuery(1L, null, START, END));

    assertThat(projection.days()).hasSize(2);
    assertThat(projection.days().get(0).date()).isEqualTo(past);
    assertThat(projection.days().get(0).status()).isEqualTo(CashFlowStatus.OVERDUE);
    assertThat(projection.days().get(1).date()).isEqualTo(future);
    assertThat(projection.days().get(1).status()).isEqualTo(CashFlowStatus.SCHEDULED);
  }

  @Test
  void treatsTodayAsScheduledNotOverdue() {
    when(repository.forecastByCompany(any()))
        .thenReturn(
            List.of(new CashFlowAggregateRow(TODAY, new BigDecimal("10.00"), BigDecimal.ZERO)));
    when(repository.actualByCompany(any())).thenReturn(List.of());

    var projection = useCase.execute(new CashFlowQuery(1L, null, START, END));

    assertThat(projection.days().getFirst().status()).isEqualTo(CashFlowStatus.SCHEDULED);
  }

  @Test
  void leavesStatusNullForDatesWithOnlyActualMovements() {
    when(repository.forecastByCompany(any())).thenReturn(List.of());
    when(repository.actualByCompany(any()))
        .thenReturn(
            List.of(new CashFlowAggregateRow(TODAY, new BigDecimal("30.00"), BigDecimal.ZERO)));

    var projection = useCase.execute(new CashFlowQuery(1L, null, START, END));

    assertThat(projection.days().getFirst().status()).isNull();
  }

  @Test
  void mergesForecastAndActualPerDateAndComputesNetAndTotals() {
    LocalDate day1 = LocalDate.of(2026, 8, 10);
    LocalDate day2 = LocalDate.of(2026, 8, 15);
    when(repository.forecastByCompany(any()))
        .thenReturn(
            List.of(
                new CashFlowAggregateRow(day1, new BigDecimal("200.00"), new BigDecimal("50.00"))));
    when(repository.actualByCompany(any()))
        .thenReturn(
            List.of(
                new CashFlowAggregateRow(day1, BigDecimal.ZERO, new BigDecimal("30.00")),
                new CashFlowAggregateRow(day2, new BigDecimal("70.00"), BigDecimal.ZERO)));

    var projection = useCase.execute(new CashFlowQuery(1L, null, START, END));

    assertThat(projection.days()).hasSize(2);
    var first = projection.days().get(0);
    assertThat(first.date()).isEqualTo(day1);
    assertThat(first.forecastInflow()).isEqualByComparingTo("200.00");
    assertThat(first.forecastOutflow()).isEqualByComparingTo("50.00");
    assertThat(first.forecastNet()).isEqualByComparingTo("150.00");
    assertThat(first.actualInflow()).isEqualByComparingTo("0.00");
    assertThat(first.actualOutflow()).isEqualByComparingTo("30.00");
    assertThat(first.actualNet()).isEqualByComparingTo("-30.00");
    var second = projection.days().get(1);
    assertThat(second.date()).isEqualTo(day2);
    assertThat(second.forecastInflow()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(second.actualInflow()).isEqualByComparingTo("70.00");

    var summary = projection.summary();
    assertThat(summary.totalForecastInflow()).isEqualByComparingTo("200.00");
    assertThat(summary.totalForecastOutflow()).isEqualByComparingTo("50.00");
    assertThat(summary.totalForecastNet()).isEqualByComparingTo("150.00");
    assertThat(summary.totalActualInflow()).isEqualByComparingTo("70.00");
    assertThat(summary.totalActualOutflow()).isEqualByComparingTo("30.00");
    assertThat(summary.totalActualNet()).isEqualByComparingTo("40.00");
  }

  @Test
  void daysAreOrderedChronologically() {
    LocalDate later = LocalDate.of(2026, 8, 20);
    LocalDate earlier = LocalDate.of(2026, 8, 5);
    when(repository.forecastByCompany(any()))
        .thenReturn(
            List.of(
                new CashFlowAggregateRow(later, BigDecimal.ONE, BigDecimal.ZERO),
                new CashFlowAggregateRow(earlier, BigDecimal.ONE, BigDecimal.ZERO)));
    when(repository.actualByCompany(any())).thenReturn(List.of());

    var projection = useCase.execute(new CashFlowQuery(1L, null, START, END));

    assertThat(projection.days()).extracting(CashFlowDay::date).containsExactly(earlier, later);
  }
}
