package com.financeiro.paymentmethod.interfaces.rest;

import com.financeiro.company.interfaces.rest.*;
import com.financeiro.paymentmethod.application.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/companies/{companyId}/payment-methods")
public class PaymentMethodController {
  private final CreatePaymentMethod create;
  private final GetPaymentMethod get;
  private final ListPaymentMethodsByCompany list;
  private final DeactivatePaymentMethod deactivate;
  private final PageQueryParser pages;

  public PaymentMethodController(
      CreatePaymentMethod create,
      GetPaymentMethod get,
      ListPaymentMethodsByCompany list,
      DeactivatePaymentMethod deactivate,
      PageQueryParser pages) {
    this.create = create;
    this.get = get;
    this.list = list;
    this.deactivate = deactivate;
    this.pages = pages;
  }

  @PostMapping
  public ResponseEntity<PaymentMethodResponse> create(
      @PathVariable @Positive Long companyId,
      @Valid @RequestBody CreatePaymentMethodRequest request) {
    var r = PaymentMethodResponse.from(create.execute(companyId, request.name()));
    return ResponseEntity.created(
            URI.create("/api/v1/companies/" + companyId + "/payment-methods/" + r.id()))
        .body(r);
  }

  @GetMapping("/{paymentMethodId}")
  public PaymentMethodResponse get(
      @PathVariable @Positive Long companyId, @PathVariable @Positive Long paymentMethodId) {
    return PaymentMethodResponse.from(get.execute(companyId, paymentMethodId));
  }

  @GetMapping
  public PageResponse<PaymentMethodResponse> list(
      @PathVariable @Positive Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    var r = list.execute(companyId, pages.parse(page, size, sort));
    return new PageResponse<>(
        r.data().stream().map(PaymentMethodResponse::from).toList(),
        new PageMetaResponse(r.page(), r.size(), r.totalElements(), r.totalPages()));
  }

  @PostMapping("/{paymentMethodId}/deactivate")
  public PaymentMethodResponse deactivate(
      @PathVariable @Positive Long companyId, @PathVariable @Positive Long paymentMethodId) {
    return PaymentMethodResponse.from(deactivate.execute(companyId, paymentMethodId));
  }
}
