package com.financeiro.paymentmethod.infrastructure.persistence;

import com.financeiro.company.application.*;
import com.financeiro.paymentmethod.application.PaymentMethodRepository;
import com.financeiro.paymentmethod.domain.PaymentMethod;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaPaymentMethodRepositoryAdapter implements PaymentMethodRepository {
  private final SpringDataPaymentMethodRepository repository;

  public JpaPaymentMethodRepositoryAdapter(SpringDataPaymentMethodRepository repository) {
    this.repository = repository;
  }

  public PaymentMethod save(PaymentMethod v) {
    return map(
        repository.save(new PaymentMethodJpaEntity(v.id(), v.companyId(), v.name(), v.active())));
  }

  public Optional<PaymentMethod> findByCompanyIdAndId(Long c, Long id) {
    return repository.findByCompanyIdAndId(c, id).map(JpaPaymentMethodRepositoryAdapter::map);
  }

  public PageResult<PaymentMethod> findPageByCompanyId(Long c, PageQuery q) {
    var p = repository.findByCompanyId(c, PageRequest.of(q.page(), q.size(), sort(q)));
    return new PageResult<>(
        p.map(JpaPaymentMethodRepositoryAdapter::map).getContent(),
        p.getNumber(),
        p.getSize(),
        p.getTotalElements(),
        p.getTotalPages());
  }

  private static PaymentMethod map(PaymentMethodJpaEntity e) {
    return PaymentMethod.rehydrate(e.id(), e.companyId(), e.name(), e.active());
  }

  private static Sort sort(PageQuery q) {
    String property = q.sortField() == PageQuery.SortField.ID ? "id" : "name";
    var d = q.direction() == PageQuery.SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
    var s = Sort.by(d, property);
    return property.equals("name") ? s.and(Sort.by(d, "id")) : s;
  }
}
