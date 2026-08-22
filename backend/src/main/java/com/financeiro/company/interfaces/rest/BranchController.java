package com.financeiro.company.interfaces.rest;

import com.financeiro.company.application.CreateBranch;
import com.financeiro.company.application.GetBranch;
import com.financeiro.company.application.ListBranchesByCompany;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/branches")
@Validated
public class BranchController {
  private final CreateBranch create;
  private final GetBranch get;
  private final ListBranchesByCompany list;
  private final PageQueryParser pages;

  public BranchController(
      CreateBranch create, GetBranch get, ListBranchesByCompany list, PageQueryParser pages) {
    this.create = create;
    this.get = get;
    this.list = list;
    this.pages = pages;
  }

  @PostMapping
  public ResponseEntity<BranchResponse> create(
      @PathVariable @Positive Long companyId, @Valid @RequestBody CreateBranchRequest request) {
    var response = BranchResponse.from(create.execute(companyId, request.name()));
    return ResponseEntity.created(
            URI.create("/api/v1/companies/" + companyId + "/branches/" + response.id()))
        .body(response);
  }

  @GetMapping("/{branchId}")
  public BranchResponse get(
      @PathVariable @Positive Long companyId, @PathVariable @Positive Long branchId) {
    return BranchResponse.from(get.execute(companyId, branchId));
  }

  @GetMapping
  public PageResponse<BranchResponse> list(
      @PathVariable @Positive Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    var result = list.execute(companyId, pages.parse(page, size, sort));
    return new PageResponse<>(
        result.data().stream().map(BranchResponse::from).toList(),
        new PageMetaResponse(
            result.page(), result.size(), result.totalElements(), result.totalPages()));
  }
}
