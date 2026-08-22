package com.financeiro.partner.application;

import java.util.List;

public record PartnerPageResult<T>(
    List<T> data, int page, int size, long totalElements, int totalPages) {
  public PartnerPageResult {
    data = List.copyOf(data);
  }
}
