package com.financeiro.history.interfaces.rest;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.financeiro.approval.domain.ApprovalDecisionType;
import com.financeiro.financialaccount.application.FinancialAccountNotFoundException;
import com.financeiro.financialmovement.domain.FinancialMovementType;
import com.financeiro.history.application.GetFinancialAccountHistory;
import com.financeiro.history.application.TimelineEntry;
import com.financeiro.history.application.TimelineEntryType;
import com.financeiro.interfaces.rest.error.GlobalExceptionHandler;
import com.financeiro.interfaces.rest.error.TraceIdProvider;
import com.financeiro.interfaces.rest.trace.TraceIdFilter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FinancialAccountHistoryController.class)
@ActiveProfiles("test")
@Import({GlobalExceptionHandler.class, TraceIdProvider.class, TraceIdFilter.class})
class FinancialAccountHistoryControllerTest {
  private static final String ROUTE = "/api/v1/companies/1/financial-accounts/10/history";
  @Autowired MockMvc mvc;
  @MockitoBean GetFinancialAccountHistory history;

  @Test
  void returnsOrderedTimelineWithSparseFieldsPerType() throws Exception {
    when(history.execute(1L, 10L))
        .thenReturn(
            List.of(
                new TimelineEntry(
                    TimelineEntryType.CREATED,
                    Instant.parse("2026-08-22T10:00:00Z"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null),
                new TimelineEntry(
                    TimelineEntryType.APPROVAL_DECISION,
                    Instant.parse("2026-08-22T11:00:00Z"),
                    "approver",
                    ApprovalDecisionType.APPROVED,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null),
                new TimelineEntry(
                    TimelineEntryType.FINANCIAL_MOVEMENT,
                    Instant.parse("2026-08-22T12:00:00Z"),
                    null,
                    null,
                    null,
                    50L,
                    null,
                    FinancialMovementType.PAYMENT,
                    new BigDecimal("40.00"),
                    LocalDate.of(2026, 8, 22))));

    mvc.perform(get(ROUTE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].type").value("CREATED"))
        .andExpect(jsonPath("$[0].occurredAt").value("2026-08-22T10:00:00Z"))
        .andExpect(jsonPath("$[0].actorId").doesNotExist())
        .andExpect(jsonPath("$[1].type").value("APPROVAL_DECISION"))
        .andExpect(jsonPath("$[1].actorId").value("approver"))
        .andExpect(jsonPath("$[1].decision").value("APPROVED"))
        .andExpect(jsonPath("$[2].type").value("FINANCIAL_MOVEMENT"))
        .andExpect(jsonPath("$[2].movementId").value(50))
        .andExpect(jsonPath("$[2].movementType").value("PAYMENT"))
        .andExpect(jsonPath("$[2].amount").value(40.0))
        .andExpect(jsonPath("$[2].movementDate").value("2026-08-22"));
  }

  @Test
  void rejectsNonPositivePathIdentifiers() throws Exception {
    for (String route :
        List.of(
            "/api/v1/companies/0/financial-accounts/10/history",
            "/api/v1/companies/-1/financial-accounts/10/history",
            "/api/v1/companies/1/financial-accounts/0/history",
            "/api/v1/companies/1/financial-accounts/-1/history")) {
      mvc.perform(get(route))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
    verifyNoInteractions(history);
  }

  @Test
  void mapsFinancialAccountNotFoundWithTraceId() throws Exception {
    when(history.execute(anyLong(), anyLong()))
        .thenThrow(new FinancialAccountNotFoundException(10L));

    mvc.perform(get(ROUTE))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("FINANCIAL_ACCOUNT_NOT_FOUND"))
        .andExpect(jsonPath("$.traceId").exists());
  }
}
