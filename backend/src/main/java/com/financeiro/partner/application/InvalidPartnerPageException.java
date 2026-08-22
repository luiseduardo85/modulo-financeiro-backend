package com.financeiro.partner.application;

public final class InvalidPartnerPageException extends RuntimeException {
  public InvalidPartnerPageException() {
    super("Paginação inválida.");
  }
}
