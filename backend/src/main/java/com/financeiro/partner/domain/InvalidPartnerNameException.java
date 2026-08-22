package com.financeiro.partner.domain;

public final class InvalidPartnerNameException extends RuntimeException {
  public InvalidPartnerNameException() {
    super("Nome do Partner inválido.");
  }
}
