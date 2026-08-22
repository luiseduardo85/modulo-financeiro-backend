package com.financeiro.financialmovement.interfaces.rest;

import com.financeiro.financialmovement.application.ReverseFinancialMovement;
import com.financeiro.financialmovement.application.ReverseFinancialMovementCommand;
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
    "/api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/installments/{installmentId}/settlements/{movementId}/reversals")
@Validated
public class ReversalController {
  private final ReverseFinancialMovement reverse;
  private final IdempotencyKeyValidator idempotencyKeys;

  public ReversalController(
      ReverseFinancialMovement reverse, IdempotencyKeyValidator idempotencyKeys) {
    this.reverse = reverse;
    this.idempotencyKeys = idempotencyKeys;
  }

  @PostMapping
  public ResponseEntity<ReversalResponse> reverse(
      @PathVariable @Positive Long companyId,
      @PathVariable @Positive Long financialAccountId,
      @PathVariable @Positive Long installmentId,
      @PathVariable @Positive Long movementId,
      @RequestHeader(value = IdempotencyHeaders.IDEMPOTENCY_KEY, required = false)
          String idempotencyKey,
      @Valid @RequestBody ReverseFinancialMovementRequest request) {
    String validKey = idempotencyKeys.validate(idempotencyKey);
    var result =
        reverse.execute(
            new ReverseFinancialMovementCommand(
                companyId,
                financialAccountId,
                installmentId,
                movementId,
                request.amount(),
                request.movementDate(),
                request.bankAccountId(),
                request.paymentMethodId(),
                validKey));
    var response = ReversalResponse.from(result);
    URI location =
        URI.create(
            "/api/v1/companies/"
                + companyId
                + "/financial-accounts/"
                + financialAccountId
                + "/installments/"
                + installmentId
                + "/settlements/"
                + movementId
                + "/reversals/"
                + response.id());
    return ResponseEntity.created(location).body(response);
  }
}
