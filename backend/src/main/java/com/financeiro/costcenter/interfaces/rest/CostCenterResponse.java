package com.financeiro.costcenter.interfaces.rest;

import com.financeiro.costcenter.domain.CostCenter;

public record CostCenterResponse(Long id, Long companyId, String name, boolean active) {
  static CostCenterResponse from(CostCenter value) {
    return new CostCenterResponse(value.id(), value.companyId(), value.name(), value.active());
  }
}
