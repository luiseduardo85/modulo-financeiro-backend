package com.financeiro.bankaccount.infrastructure.persistence;

import com.financeiro.bankaccount.application.BankAccountRepository;
import com.financeiro.bankaccount.domain.BankAccount;
import com.financeiro.company.application.*;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaBankAccountRepositoryAdapter implements BankAccountRepository {
  private final SpringDataBankAccountRepository repository;

  public JpaBankAccountRepositoryAdapter(SpringDataBankAccountRepository repository) {
    this.repository = repository;
  }

  public BankAccount save(BankAccount v) {
    return map(
        repository.save(
            new BankAccountJpaEntity(v.id(), v.companyId(), v.branchId(), v.name(), v.active())));
  }

  public Optional<BankAccount> findByCompanyIdAndId(Long c, Long id) {
    return repository.findByCompanyIdAndId(c, id).map(JpaBankAccountRepositoryAdapter::map);
  }

  public PageResult<BankAccount> findPageByCompanyId(Long c, PageQuery q) {
    var p = repository.findByCompanyId(c, PageRequest.of(q.page(), q.size(), sort(q)));
    return new PageResult<>(
        p.map(JpaBankAccountRepositoryAdapter::map).getContent(),
        p.getNumber(),
        p.getSize(),
        p.getTotalElements(),
        p.getTotalPages());
  }

  private static BankAccount map(BankAccountJpaEntity e) {
    return BankAccount.rehydrate(e.id(), e.companyId(), e.branchId(), e.name(), e.active());
  }

  private static Sort sort(PageQuery q) {
    String property = q.sortField() == PageQuery.SortField.ID ? "id" : "name";
    var d = q.direction() == PageQuery.SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
    var s = Sort.by(d, property);
    return property.equals("name") ? s.and(Sort.by(d, "id")) : s;
  }
}
