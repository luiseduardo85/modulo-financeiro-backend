package com.financeiro.company.application;

public record PageQuery(int page, int size, SortField sortField, SortDirection direction) {
  public PageQuery {
    if (page < 0) throw new InvalidPageRequestException("page must be at least 0");
    if (size < 1 || size > 100)
      throw new InvalidPageRequestException("size must be between 1 and 100");
    if (sortField == null || direction == null)
      throw new InvalidPageRequestException("sort is invalid");
  }

  public enum SortField {
    ID,
    NAME
  }

  public enum SortDirection {
    ASC,
    DESC
  }
}
