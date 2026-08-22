package com.financeiro.partner.application;

public final class PartnerNotFoundException extends RuntimeException {
  public PartnerNotFoundException(Long id) {
    super("Partner não encontrado: " + id);
  }
}
