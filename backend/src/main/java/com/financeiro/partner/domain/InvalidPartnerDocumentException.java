package com.financeiro.partner.domain;

public final class InvalidPartnerDocumentException extends RuntimeException {
  public InvalidPartnerDocumentException() {
    super("Documento CPF/CNPJ inválido.");
  }
}
