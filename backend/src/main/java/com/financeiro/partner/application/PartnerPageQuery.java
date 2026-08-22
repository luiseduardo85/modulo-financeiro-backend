package com.financeiro.partner.application;

public record PartnerPageQuery(int page, int size, SortField field, Direction direction) {
  public PartnerPageQuery {
    if (page < 0 || size < 1 || size > 100 || field == null || direction == null)
      throw new InvalidPartnerPageException();
  }

  public enum SortField {
    ID,
    NAME
  }

  public enum Direction {
    ASC,
    DESC
  }
}
