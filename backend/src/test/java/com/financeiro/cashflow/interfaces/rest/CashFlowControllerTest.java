package com.financeiro.cashflow.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.financeiro.cashflow.application.*;
import com.financeiro.cashflow.domain.CashFlowStatus;
import com.financeiro.company.application.BranchNotFoundException;
import com.financeiro.company.application.CompanyNotFoundException;
import com.financeiro.interfaces.rest.error.GlobalExceptionHandler;
import com.financeiro.interfaces.rest.error.TraceIdProvider;
import com.financeiro.interfaces.rest.trace.TraceIdFilter;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CashFlowController.class)
@ActiveProfiles("test")
@Import({GlobalExceptionHandler.class, TraceIdProvider.class, TraceIdFilter.class})
class CashFlowControllerTest {
  private static final String ROUTE = "/api/v1/companies/1/cash-flow";
  @Autowired MockMvc mvc;
  @MockitoBean GetCashFlow getCashFlow;

  @Test
  void returnsProjectedDaysAndSummary() throws Exception {
    var day =
        new CashFlowDay(
            java.time.LocalDate.of(2026, 8, 25),
            CashFlowStatus.SCHEDULED,
            new BigDecimal("100.00"),
            new BigDecimal("40.00"),
            new BigDecimal("60.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO);
    var summary =
        new CashFlowSummary(
            new BigDecimal("100.00"),
            new BigDecimal("40.00"),
            new BigDecimal("60.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO);
    when(getCashFlow.execute(any())).thenReturn(new CashFlowProjection(List.of(day), summary));

    mvc.perform(get(ROUTE).param("startDate", "2026-08-01").param("endDate", "2026-08-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days.length()").value(1))
        .andExpect(jsonPath("$.days[0].date").value("2026-08-25"))
        .andExpect(jsonPath("$.days[0].status").value("SCHEDULED"))
        .andExpect(jsonPath("$.days[0].forecastInflow").value(100.0))
        .andExpect(jsonPath("$.days[0].forecastOutflow").value(40.0))
        .andExpect(jsonPath("$.days[0].forecastNet").value(60.0))
        .andExpect(jsonPath("$.summary.totalForecastInflow").value(100.0))
        .andExpect(jsonPath("$.summary.totalForecastNet").value(60.0));
  }

  @Test
  void passesOptionalBranchIdWhenPresent() throws Exception {
    when(getCashFlow.execute(any())).thenReturn(new CashFlowProjection(List.of(), zeroSummary()));

    mvc.perform(
            get(ROUTE)
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31")
                .param("branchId", "2"))
        .andExpect(status().isOk());

    verify(getCashFlow)
        .execute(
            new CashFlowQuery(
                1L, 2L, java.time.LocalDate.of(2026, 8, 1), java.time.LocalDate.of(2026, 8, 31)));
  }

  @Test
  void requiresStartAndEndDate() throws Exception {
    mvc.perform(get(ROUTE).param("endDate", "2026-08-31"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mvc.perform(get(ROUTE).param("startDate", "2026-08-01"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    verifyNoInteractions(getCashFlow);
  }

  @Test
  void rejectsMalformedDates() throws Exception {
    mvc.perform(get(ROUTE).param("startDate", "not-a-date").param("endDate", "2026-08-31"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    verifyNoInteractions(getCashFlow);
  }

  @Test
  void rejectsNonPositivePathAndBranchIds() throws Exception {
    for (String route :
        List.of("/api/v1/companies/0/cash-flow", "/api/v1/companies/-1/cash-flow")) {
      mvc.perform(get(route).param("startDate", "2026-08-01").param("endDate", "2026-08-31"))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
    mvc.perform(
            get(ROUTE)
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31")
                .param("branchId", "0"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    verifyNoInteractions(getCashFlow);
  }

  @Test
  void mapsCompanyAndBranchNotFoundWithTraceId() throws Exception {
    when(getCashFlow.execute(any())).thenThrow(new CompanyNotFoundException(1L));
    mvc.perform(get(ROUTE).param("startDate", "2026-08-01").param("endDate", "2026-08-31"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"))
        .andExpect(jsonPath("$.traceId").exists());

    reset(getCashFlow);
    when(getCashFlow.execute(any())).thenThrow(new BranchNotFoundException(2L));
    mvc.perform(
            get(ROUTE)
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31")
                .param("branchId", "2"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BRANCH_NOT_FOUND"));
  }

  @Test
  void mapsInvertedDateRangeAsValidationErrorWithoutInvokingUseCase() throws Exception {
    mvc.perform(get(ROUTE).param("startDate", "2026-08-31").param("endDate", "2026-08-01"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    verifyNoInteractions(getCashFlow);
  }

  private static CashFlowSummary zeroSummary() {
    return new CashFlowSummary(
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO);
  }
}
