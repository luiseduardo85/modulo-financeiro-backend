package com.financeiro.costcenter.domain;

public final class InvalidCostCenterNameException extends RuntimeException {
  public InvalidCostCenterNameException() {
    super("Nome de CostCenter inválido.");
  }
}
