package com.financeiro.company.interfaces.rest;

import com.financeiro.company.application.CreateCompany;
import com.financeiro.company.application.GetCompany;
import com.financeiro.company.application.ListCompanies;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/companies")
@Validated
public class CompanyController {
  private final CreateCompany create;
  private final GetCompany get;
  private final ListCompanies list;
  private final PageQueryParser pages;

  public CompanyController(
      CreateCompany create, GetCompany get, ListCompanies list, PageQueryParser pages) {
    this.create = create;
    this.get = get;
    this.list = list;
    this.pages = pages;
  }

  @PostMapping
  public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CreateCompanyRequest request) {
    var response = CompanyResponse.from(create.execute(request.name()));
    return ResponseEntity.created(URI.create("/api/v1/companies/" + response.id())).body(response);
  }

  @GetMapping("/{id}")
  public CompanyResponse get(@PathVariable @Positive Long id) {
    return CompanyResponse.from(get.execute(id));
  }

  @GetMapping
  public PageResponse<CompanyResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    var result = list.execute(pages.parse(page, size, sort));
    return new PageResponse<>(
        result.data().stream().map(CompanyResponse::from).toList(),
        new PageMetaResponse(
            result.page(), result.size(), result.totalElements(), result.totalPages()));
  }
}
