package com.financeiro.cashflow.application;

import com.financeiro.cashflow.domain.CashFlowStatus;
import com.financeiro.company.application.BranchNotFoundException;
import com.financeiro.company.application.BranchRepository;
import com.financeiro.company.application.CompanyNotFoundException;
import com.financeiro.company.application.CompanyRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCashFlow {
  private final CompanyRepository companies;
  private final BranchRepository branches;
  private final CashFlowRepository cashFlow;
  private final Clock clock;

  public GetCashFlow(
      CompanyRepository companies,
      BranchRepository branches,
      CashFlowRepository cashFlow,
      Clock clock) {
    this.companies = companies;
    this.branches = branches;
    this.cashFlow = cashFlow;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public CashFlowProjection execute(CashFlowQuery query) {
    companies
        .findById(query.companyId())
        .orElseThrow(() -> new CompanyNotFoundException(query.companyId()));
    if (query.branchId() != null) {
      branches
          .findByCompanyIdAndId(query.companyId(), query.branchId())
          .orElseThrow(() -> new BranchNotFoundException(query.branchId()));
    }

    Map<LocalDate, CashFlowAggregateRow> forecastByDate =
        cashFlow.forecastByCompany(query).stream()
            .collect(Collectors.toMap(CashFlowAggregateRow::date, Function.identity()));
    Map<LocalDate, CashFlowAggregateRow> actualByDate =
        cashFlow.actualByCompany(query).stream()
            .collect(Collectors.toMap(CashFlowAggregateRow::date, Function.identity()));

    var dates = new TreeSet<LocalDate>();
    dates.addAll(forecastByDate.keySet());
    dates.addAll(actualByDate.keySet());

    LocalDate today = LocalDate.now(clock);
    List<CashFlowDay> days = new ArrayList<>();
    BigDecimal totalForecastInflow = BigDecimal.ZERO;
    BigDecimal totalForecastOutflow = BigDecimal.ZERO;
    BigDecimal totalActualInflow = BigDecimal.ZERO;
    BigDecimal totalActualOutflow = BigDecimal.ZERO;

    for (LocalDate date : dates) {
      CashFlowAggregateRow forecast = forecastByDate.get(date);
      CashFlowAggregateRow actual = actualByDate.get(date);
      BigDecimal forecastInflow = forecast != null ? forecast.inflow() : BigDecimal.ZERO;
      BigDecimal forecastOutflow = forecast != null ? forecast.outflow() : BigDecimal.ZERO;
      BigDecimal actualInflow = actual != null ? actual.inflow() : BigDecimal.ZERO;
      BigDecimal actualOutflow = actual != null ? actual.outflow() : BigDecimal.ZERO;
      CashFlowStatus status =
          forecast == null
              ? null
              : date.isBefore(today) ? CashFlowStatus.OVERDUE : CashFlowStatus.SCHEDULED;

      days.add(
          new CashFlowDay(
              date,
              status,
              forecastInflow,
              forecastOutflow,
              forecastInflow.subtract(forecastOutflow),
              actualInflow,
              actualOutflow,
              actualInflow.subtract(actualOutflow)));

      totalForecastInflow = totalForecastInflow.add(forecastInflow);
      totalForecastOutflow = totalForecastOutflow.add(forecastOutflow);
      totalActualInflow = totalActualInflow.add(actualInflow);
      totalActualOutflow = totalActualOutflow.add(actualOutflow);
    }

    var summary =
        new CashFlowSummary(
            totalForecastInflow,
            totalForecastOutflow,
            totalForecastInflow.subtract(totalForecastOutflow),
            totalActualInflow,
            totalActualOutflow,
            totalActualInflow.subtract(totalActualOutflow));

    return new CashFlowProjection(days, summary);
  }
}
