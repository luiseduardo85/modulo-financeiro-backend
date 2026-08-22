package com.financeiro.cashflow.interfaces.rest;

import com.financeiro.cashflow.application.CashFlowQuery;
import com.financeiro.cashflow.application.GetCashFlow;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/cash-flow")
@Validated
public class CashFlowController {
  private final GetCashFlow getCashFlow;

  public CashFlowController(GetCashFlow getCashFlow) {
    this.getCashFlow = getCashFlow;
  }

  @GetMapping
  public CashFlowResponse cashFlow(
      @PathVariable @Positive Long companyId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) @Positive Long branchId) {
    var query = new CashFlowQuery(companyId, branchId, startDate, endDate);
    return CashFlowResponse.from(getCashFlow.execute(query));
  }
}
