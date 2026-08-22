package com.financeiro.partner.interfaces.rest;

import com.financeiro.partner.application.*;
import org.springframework.stereotype.Component;

@Component
public final class PartnerPageQueryParser {
  public PartnerPageQuery parse(int page, int size, String sort) {
    String[] p = sort == null ? new String[0] : sort.split(",", -1);
    if (p.length != 2) throw new InvalidPartnerPageException();
    PartnerPageQuery.SortField f =
        switch (p[0]) {
          case "id" -> PartnerPageQuery.SortField.ID;
          case "name" -> PartnerPageQuery.SortField.NAME;
          default -> throw new InvalidPartnerPageException();
        };
    PartnerPageQuery.Direction d =
        switch (p[1]) {
          case "asc" -> PartnerPageQuery.Direction.ASC;
          case "desc" -> PartnerPageQuery.Direction.DESC;
          default -> throw new InvalidPartnerPageException();
        };
    return new PartnerPageQuery(page, size, f, d);
  }
}
