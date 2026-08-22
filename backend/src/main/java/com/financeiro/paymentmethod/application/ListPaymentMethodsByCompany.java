package com.financeiro.paymentmethod.application;

import com.financeiro.company.application.*;
import com.financeiro.paymentmethod.domain.PaymentMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListPaymentMethodsByCompany {
  private final CompanyRepository companies;
  private final PaymentMethodRepository repository;

  public ListPaymentMethodsByCompany(
      CompanyRepository companies, PaymentMethodRepository repository) {
    this.companies = companies;
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public PageResult<PaymentMethod> execute(Long companyId, PageQuery query) {
    companies.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    return repository.findPageByCompanyId(companyId, query);
  }
}
