package com.financeiro.costcenter.application;

public final class CostCenterNotFoundException extends RuntimeException {
  public CostCenterNotFoundException(Long companyId, Long costCenterId) {
    super("Cost center not found.");
  }
}
