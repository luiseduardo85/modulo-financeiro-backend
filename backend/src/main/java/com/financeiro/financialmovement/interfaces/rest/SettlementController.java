package com.financeiro.financialmovement.interfaces.rest;

import com.financeiro.financialmovement.application.SettleInstallment;
import com.financeiro.financialmovement.application.SettleInstallmentCommand;
import com.financeiro.idempotency.interfaces.rest.IdempotencyHeaders;
import com.financeiro.idempotency.interfaces.rest.IdempotencyKeyValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
    "/api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/installments/{installmentId}/settlements")
@Validated
public class SettlementController {
  private final SettleInstallment settle;
  private final IdempotencyKeyValidator idempotencyKeys;

  public SettlementController(SettleInstallment settle, IdempotencyKeyValidator idempotencyKeys) {
    this.settle = settle;
    this.idempotencyKeys = idempotencyKeys;
  }

  @PostMapping
  public ResponseEntity<SettlementResponse> settle(
      @PathVariable @Positive Long companyId,
      @PathVariable @Positive Long financialAccountId,
      @PathVariable @Positive Long installmentId,
      @RequestHeader(value = IdempotencyHeaders.IDEMPOTENCY_KEY, required = false)
          String idempotencyKey,
      @Valid @RequestBody SettleInstallmentRequest request) {
    String validKey = idempotencyKeys.validate(idempotencyKey);
    var result =
        settle.execute(
            new SettleInstallmentCommand(
                companyId,
                financialAccountId,
                installmentId,
                request.amount(),
                request.movementDate(),
                request.bankAccountId(),
                request.paymentMethodId(),
                validKey));
    var response = SettlementResponse.from(result);
    URI location =
        URI.create(
            "/api/v1/companies/"
                + companyId
                + "/financial-accounts/"
                + financialAccountId
                + "/installments/"
                + installmentId
                + "/settlements/"
                + response.id());
    return ResponseEntity.created(location).body(response);
  }
}
