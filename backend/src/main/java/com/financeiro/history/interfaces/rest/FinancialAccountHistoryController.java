package com.financeiro.history.interfaces.rest;

import com.financeiro.history.application.GetFinancialAccountHistory;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/history")
@Validated
public class FinancialAccountHistoryController {
  private final GetFinancialAccountHistory history;

  public FinancialAccountHistoryController(GetFinancialAccountHistory history) {
    this.history = history;
  }

  @GetMapping
  public List<TimelineEntryResponse> history(
      @PathVariable @Positive Long companyId, @PathVariable @Positive Long financialAccountId) {
    return history.execute(companyId, financialAccountId).stream()
        .map(TimelineEntryResponse::from)
        .toList();
  }
}
