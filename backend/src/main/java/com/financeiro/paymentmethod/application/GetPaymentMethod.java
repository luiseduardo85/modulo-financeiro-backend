package com.financeiro.paymentmethod.application;

import com.financeiro.paymentmethod.domain.PaymentMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPaymentMethod {
  private final PaymentMethodRepository repository;

  public GetPaymentMethod(PaymentMethodRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public PaymentMethod execute(Long companyId, Long id) {
    return repository
        .findByCompanyIdAndId(companyId, id)
        .orElseThrow(() -> new PaymentMethodNotFoundException(companyId, id));
  }
}
