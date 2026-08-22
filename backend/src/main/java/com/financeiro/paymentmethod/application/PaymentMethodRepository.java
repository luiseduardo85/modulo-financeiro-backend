package com.financeiro.paymentmethod.application;

import com.financeiro.company.application.PageQuery;
import com.financeiro.company.application.PageResult;
import com.financeiro.paymentmethod.domain.PaymentMethod;
import java.util.Optional;

public interface PaymentMethodRepository {
  PaymentMethod save(PaymentMethod value);

  Optional<PaymentMethod> findByCompanyIdAndId(Long companyId, Long id);

  PageResult<PaymentMethod> findPageByCompanyId(Long companyId, PageQuery query);
}
