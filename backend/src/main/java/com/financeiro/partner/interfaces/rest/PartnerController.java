package com.financeiro.partner.interfaces.rest;

import com.financeiro.partner.application.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/partners")
@Validated
public class PartnerController {
  private final CreatePartner create;
  private final GetPartner get;
  private final ListPartners list;
  private final DeactivatePartner deactivate;
  private final PartnerPageQueryParser pages;

  public PartnerController(
      CreatePartner create,
      GetPartner get,
      ListPartners list,
      DeactivatePartner deactivate,
      PartnerPageQueryParser pages) {
    this.create = create;
    this.get = get;
    this.list = list;
    this.deactivate = deactivate;
    this.pages = pages;
  }

  @PostMapping
  public ResponseEntity<PartnerResponse> create(@Valid @RequestBody CreatePartnerRequest r) {
    PartnerResponse body = PartnerResponse.from(create.execute(r.name(), r.document(), r.roles()));
    return ResponseEntity.created(URI.create("/api/v1/partners/" + body.id())).body(body);
  }

  @GetMapping("/{id}")
  public PartnerResponse get(@PathVariable @Positive Long id) {
    return PartnerResponse.from(get.execute(id));
  }

  @GetMapping
  public PartnerPageResponse list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    var result = list.execute(pages.parse(page, size, sort));
    return new PartnerPageResponse(
        result.data().stream().map(PartnerResponse::from).toList(),
        new PartnerPageMetaResponse(
            result.page(), result.size(), result.totalElements(), result.totalPages()));
  }

  @PostMapping("/{id}/deactivate")
  public PartnerResponse deactivate(@PathVariable @Positive Long id) {
    return PartnerResponse.from(deactivate.execute(id));
  }
}
