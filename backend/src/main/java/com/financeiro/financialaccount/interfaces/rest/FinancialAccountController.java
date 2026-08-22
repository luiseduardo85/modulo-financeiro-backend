package com.financeiro.financialaccount.interfaces.rest;

import com.financeiro.approval.application.*;
import com.financeiro.approval.interfaces.rest.ApprovalActionRequest;
import com.financeiro.approval.interfaces.rest.RejectFinancialAccountRequest;
import com.financeiro.company.interfaces.rest.*;
import com.financeiro.financialaccount.application.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/financial-accounts")
@Validated
public class FinancialAccountController {
  private final CreateFinancialAccount create;
  private final GetFinancialAccount get;
  private final ListFinancialAccountsByCompany list;
  private final FinancialAccountPageQueryParser pages;
  private final SubmitFinancialAccountForApproval submitForApproval;
  private final ApproveFinancialAccount approve;
  private final RejectFinancialAccount reject;

  public FinancialAccountController(
      CreateFinancialAccount create,
      GetFinancialAccount get,
      ListFinancialAccountsByCompany list,
      FinancialAccountPageQueryParser pages,
      SubmitFinancialAccountForApproval submitForApproval,
      ApproveFinancialAccount approve,
      RejectFinancialAccount reject) {
    this.create = create;
    this.get = get;
    this.list = list;
    this.pages = pages;
    this.submitForApproval = submitForApproval;
    this.approve = approve;
    this.reject = reject;
  }

  @PostMapping
  public ResponseEntity<FinancialAccountResponse> create(
      @PathVariable @Positive Long companyId,
      @Valid @RequestBody CreateFinancialAccountRequest request) {
    var command =
        new CreateFinancialAccountCommand(
            companyId,
            request.branchId(),
            request.type(),
            request.partnerId(),
            request.categoryId(),
            request.costCenterId(),
            request.issueDate(),
            request.totalAmount(),
            request.installments().stream()
                .map(
                    i ->
                        new CreateInstallmentCommand(
                            i.installmentNumber(), i.dueDate(), i.amount()))
                .toList());
    var response = FinancialAccountResponse.from(create.execute(command));
    return ResponseEntity.created(
            URI.create("/api/v1/companies/" + companyId + "/financial-accounts/" + response.id()))
        .body(response);
  }

  @GetMapping("/{financialAccountId}")
  public FinancialAccountResponse get(
      @PathVariable @Positive Long companyId, @PathVariable @Positive Long financialAccountId) {
    return FinancialAccountResponse.from(get.execute(companyId, financialAccountId));
  }

  @GetMapping
  public PageResponse<FinancialAccountSummaryResponse> list(
      @PathVariable @Positive Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    var result = list.execute(companyId, pages.parse(page, size, sort));
    return new PageResponse<>(
        result.data().stream().map(FinancialAccountSummaryResponse::from).toList(),
        new PageMetaResponse(
            result.page(), result.size(), result.totalElements(), result.totalPages()));
  }

  @PostMapping("/{financialAccountId}/submit-for-approval")
  public FinancialAccountResponse submitForApproval(
      @PathVariable @Positive Long companyId,
      @PathVariable @Positive Long financialAccountId,
      @RequestBody(required = false) ApprovalActionRequest request) {
    return FinancialAccountResponse.from(submitForApproval.execute(companyId, financialAccountId));
  }

  @PostMapping("/{financialAccountId}/approve")
  public FinancialAccountResponse approve(
      @PathVariable @Positive Long companyId,
      @PathVariable @Positive Long financialAccountId,
      @RequestBody(required = false) ApprovalActionRequest request) {
    return FinancialAccountResponse.from(approve.execute(companyId, financialAccountId));
  }

  @PostMapping("/{financialAccountId}/reject")
  public FinancialAccountResponse reject(
      @PathVariable @Positive Long companyId,
      @PathVariable @Positive Long financialAccountId,
      @RequestBody RejectFinancialAccountRequest request) {
    return FinancialAccountResponse.from(
        reject.execute(companyId, financialAccountId, request.justification()));
  }
}
