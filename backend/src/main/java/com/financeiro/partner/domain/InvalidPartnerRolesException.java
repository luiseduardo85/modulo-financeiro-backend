package com.financeiro.partner.domain;

public final class InvalidPartnerRolesException extends RuntimeException {
  public InvalidPartnerRolesException() {
    super("Partner deve possuir ao menos um papel válido.");
  }
}
