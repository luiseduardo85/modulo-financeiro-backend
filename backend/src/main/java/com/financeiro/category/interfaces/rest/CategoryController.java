package com.financeiro.category.interfaces.rest;

import com.financeiro.category.application.*;
import com.financeiro.company.interfaces.rest.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/categories")
@Validated
public class CategoryController {
  private final CreateCategory create;
  private final GetCategory get;
  private final ListCategoriesByCompany list;
  private final DeactivateCategory deactivate;
  private final PageQueryParser pages;

  public CategoryController(
      CreateCategory create,
      GetCategory get,
      ListCategoriesByCompany list,
      DeactivateCategory deactivate,
      PageQueryParser pages) {
    this.create = create;
    this.get = get;
    this.list = list;
    this.deactivate = deactivate;
    this.pages = pages;
  }

  @PostMapping
  public ResponseEntity<CategoryResponse> create(
      @PathVariable @Positive Long companyId, @Valid @RequestBody CreateCategoryRequest request) {
    var response = CategoryResponse.from(create.execute(companyId, request.name()));
    return ResponseEntity.created(
            URI.create("/api/v1/companies/" + companyId + "/categories/" + response.id()))
        .body(response);
  }

  @GetMapping("/{categoryId}")
  public CategoryResponse get(
      @PathVariable @Positive Long companyId, @PathVariable @Positive Long categoryId) {
    return CategoryResponse.from(get.execute(companyId, categoryId));
  }

  @GetMapping
  public PageResponse<CategoryResponse> list(
      @PathVariable @Positive Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    var result = list.execute(companyId, pages.parse(page, size, sort));
    return new PageResponse<>(
        result.data().stream().map(CategoryResponse::from).toList(),
        new PageMetaResponse(
            result.page(), result.size(), result.totalElements(), result.totalPages()));
  }

  @PostMapping("/{categoryId}/deactivate")
  public CategoryResponse deactivate(
      @PathVariable @Positive Long companyId, @PathVariable @Positive Long categoryId) {
    return CategoryResponse.from(deactivate.execute(companyId, categoryId));
  }
}
