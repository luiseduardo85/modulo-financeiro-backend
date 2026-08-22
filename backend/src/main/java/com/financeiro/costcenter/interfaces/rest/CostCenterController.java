package com.financeiro.costcenter.interfaces.rest;

import com.financeiro.company.interfaces.rest.*;
import com.financeiro.costcenter.application.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/cost-centers")
@Validated
public class CostCenterController {
  private final CreateCostCenter create;
  private final GetCostCenter get;
  private final ListCostCentersByCompany list;
  private final DeactivateCostCenter deactivate;
  private final PageQueryParser pages;

  public CostCenterController(
      CreateCostCenter create,
      GetCostCenter get,
      ListCostCentersByCompany list,
      DeactivateCostCenter deactivate,
      PageQueryParser pages) {
    this.create = create;
    this.get = get;
    this.list = list;
    this.deactivate = deactivate;
    this.pages = pages;
  }

  @PostMapping
  public ResponseEntity<CostCenterResponse> create(
      @PathVariable @Positive Long companyId, @Valid @RequestBody CreateCostCenterRequest request) {
    var response = CostCenterResponse.from(create.execute(companyId, request.name()));
    return ResponseEntity.created(
            URI.create("/api/v1/companies/" + companyId + "/cost-centers/" + response.id()))
        .body(response);
  }

  @GetMapping("/{costCenterId}")
  public CostCenterResponse get(
      @PathVariable @Positive Long companyId, @PathVariable @Positive Long costCenterId) {
    return CostCenterResponse.from(get.execute(companyId, costCenterId));
  }

  @GetMapping
  public PageResponse<CostCenterResponse> list(
      @PathVariable @Positive Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    var result = list.execute(companyId, pages.parse(page, size, sort));
    return new PageResponse<>(
        result.data().stream().map(CostCenterResponse::from).toList(),
        new PageMetaResponse(
            result.page(), result.size(), result.totalElements(), result.totalPages()));
  }

  @PostMapping("/{costCenterId}/deactivate")
  public CostCenterResponse deactivate(
      @PathVariable @Positive Long companyId, @PathVariable @Positive Long costCenterId) {
    return CostCenterResponse.from(deactivate.execute(companyId, costCenterId));
  }
}
