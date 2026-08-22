package com.financeiro.financialaccount.application;

public final class PartnerRoleNotAllowedException extends RuntimeException {
  public PartnerRoleNotAllowedException() {
    super("Partner não possui o role exigido");
  }
}
