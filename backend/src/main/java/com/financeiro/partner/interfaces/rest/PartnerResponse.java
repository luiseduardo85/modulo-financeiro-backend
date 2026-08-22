package com.financeiro.partner.interfaces.rest;

import com.financeiro.partner.domain.*;
import java.util.List;

public record PartnerResponse(
    Long id,
    String name,
    String document,
    DocumentType documentType,
    List<PartnerRole> roles,
    boolean active) {
  static PartnerResponse from(Partner p) {
    return new PartnerResponse(
        p.id(),
        p.name(),
        p.document().value(),
        p.document().type(),
        p.roles().stream().sorted().toList(),
        p.active());
  }
}
