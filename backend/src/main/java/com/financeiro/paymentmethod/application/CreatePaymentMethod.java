package com.financeiro.paymentmethod.application;

import com.financeiro.company.application.*;
import com.financeiro.paymentmethod.domain.PaymentMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePaymentMethod {
  private final CompanyRepository companies;
  private final PaymentMethodRepository repository;

  public CreatePaymentMethod(CompanyRepository companies, PaymentMethodRepository repository) {
    this.companies = companies;
    this.repository = repository;
  }

  @Transactional
  public PaymentMethod execute(Long companyId, String name) {
    PaymentMethod candidate = PaymentMethod.create(companyId, name);
    companies.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    return repository.save(candidate);
  }
}
