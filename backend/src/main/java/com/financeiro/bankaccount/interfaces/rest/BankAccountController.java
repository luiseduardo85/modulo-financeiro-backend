package com.financeiro.bankaccount.interfaces.rest;

import com.financeiro.bankaccount.application.*;
import com.financeiro.company.interfaces.rest.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/companies/{companyId}/bank-accounts")
public class BankAccountController {
  private final CreateBankAccount create;
  private final GetBankAccount get;
  private final ListBankAccountsByCompany list;
  private final DeactivateBankAccount deactivate;
  private final PageQueryParser pages;

  public BankAccountController(
      CreateBankAccount create,
      GetBankAccount get,
      ListBankAccountsByCompany list,
      DeactivateBankAccount deactivate,
      PageQueryParser pages) {
    this.create = create;
    this.get = get;
    this.list = list;
    this.deactivate = deactivate;
    this.pages = pages;
  }

  @PostMapping
  public ResponseEntity<BankAccountResponse> create(
      @PathVariable @Positive Long companyId,
      @Valid @RequestBody CreateBankAccountRequest request) {
    var r = BankAccountResponse.from(create.execute(companyId, request.branchId(), request.name()));
    return ResponseEntity.created(
            URI.create("/api/v1/companies/" + companyId + "/bank-accounts/" + r.id()))
        .body(r);
  }

  @GetMapping("/{bankAccountId}")
  public BankAccountResponse get(
      @PathVariable @Positive Long companyId, @PathVariable @Positive Long bankAccountId) {
    return BankAccountResponse.from(get.execute(companyId, bankAccountId));
  }

  @GetMapping
  public PageResponse<BankAccountResponse> list(
      @PathVariable @Positive Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    var r = list.execute(companyId, pages.parse(page, size, sort));
    return new PageResponse<>(
        r.data().stream().map(BankAccountResponse::from).toList(),
        new PageMetaResponse(r.page(), r.size(), r.totalElements(), r.totalPages()));
  }

  @PostMapping("/{bankAccountId}/deactivate")
  public BankAccountResponse deactivate(
      @PathVariable @Positive Long companyId, @PathVariable @Positive Long bankAccountId) {
    return BankAccountResponse.from(deactivate.execute(companyId, bankAccountId));
  }
}
