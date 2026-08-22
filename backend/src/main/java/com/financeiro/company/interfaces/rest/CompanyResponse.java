package com.financeiro.company.interfaces.rest;

import com.financeiro.company.domain.Company;

public record CompanyResponse(Long id, String name) {
  static CompanyResponse from(Company company) {
    return new CompanyResponse(company.id(), company.name());
  }
}
