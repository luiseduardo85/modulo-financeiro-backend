package com.financeiro.company.application;

import java.util.List;

public record PageResult<T>(List<T> data, int page, int size, long totalElements, int totalPages) {
  public PageResult {
    data = List.copyOf(data);
  }
}
