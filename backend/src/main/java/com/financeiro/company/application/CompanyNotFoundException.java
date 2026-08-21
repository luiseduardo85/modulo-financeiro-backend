package com.financeiro.company.application;

public final class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException(Long id) { super("Empresa não encontrada: " + id); }
}
