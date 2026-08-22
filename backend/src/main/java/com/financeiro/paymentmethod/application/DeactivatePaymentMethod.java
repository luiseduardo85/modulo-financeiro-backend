package com.financeiro.paymentmethod.application;

import com.financeiro.paymentmethod.domain.PaymentMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeactivatePaymentMethod {
  private final PaymentMethodRepository repository;

  public DeactivatePaymentMethod(PaymentMethodRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public PaymentMethod execute(Long companyId, Long id) {
    var value =
        repository
            .findByCompanyIdAndId(companyId, id)
            .orElseThrow(() -> new PaymentMethodNotFoundException(companyId, id));
    value.deactivate();
    return repository.save(value);
  }
}
