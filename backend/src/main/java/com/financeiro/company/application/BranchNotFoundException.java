package com.financeiro.company.application;

public final class BranchNotFoundException extends RuntimeException {
  public BranchNotFoundException(Long id) {
    super("Filial não encontrada: " + id);
  }
}
