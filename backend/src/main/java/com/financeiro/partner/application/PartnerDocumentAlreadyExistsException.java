package com.financeiro.partner.application;

public final class PartnerDocumentAlreadyExistsException extends RuntimeException {
  public PartnerDocumentAlreadyExistsException() {
    super("Já existe Partner com o documento informado.");
  }
}
