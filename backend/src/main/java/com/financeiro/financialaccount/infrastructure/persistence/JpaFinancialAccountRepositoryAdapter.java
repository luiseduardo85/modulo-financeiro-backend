package com.financeiro.financialaccount.infrastructure.persistence;

import com.financeiro.company.application.PageResult;
import com.financeiro.financialaccount.application.*;
import com.financeiro.financialaccount.domain.FinancialAccount;
import com.financeiro.financialaccount.domain.Installment;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaFinancialAccountRepositoryAdapter implements FinancialAccountRepository {
  private final SpringDataFinancialAccountRepository repository;

  public JpaFinancialAccountRepositoryAdapter(SpringDataFinancialAccountRepository repository) {
    this.repository = repository;
  }

  @Override
  public FinancialAccount save(FinancialAccount value) {
    return toDomain(repository.save(new FinancialAccountJpaEntity(value)));
  }

  @Override
  public Optional<FinancialAccount> findByCompanyIdAndId(Long companyId, Long id) {
    return repository
        .findByCompanyIdAndId(companyId, id)
        .map(JpaFinancialAccountRepositoryAdapter::toDomain);
  }

  @Override
  public PageResult<FinancialAccountSummary> findSummaryPageByCompanyId(
      Long companyId, FinancialAccountPageQuery query) {
    var direction =
        query.direction() == FinancialAccountPageQuery.Direction.ASC
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
    var page =
        repository.findByCompanyId(
            companyId, PageRequest.of(query.page(), query.size(), Sort.by(direction, "id")));
    return new PageResult<>(
        page.map(JpaFinancialAccountRepositoryAdapter::toSummary).getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }

  private static FinancialAccount toDomain(FinancialAccountJpaEntity e) {
    return FinancialAccount.rehydrate(
        e.id(),
        e.companyId(),
        e.branchId(),
        e.type(),
        e.partnerId(),
        e.categoryId(),
        e.costCenterId(),
        e.issueDate(),
        e.totalAmount(),
        e.status(),
        e.installments().stream()
            .map(i -> Installment.rehydrate(i.id(), i.installmentNumber(), i.dueDate(), i.amount()))
            .toList());
  }

  private static FinancialAccountSummary toSummary(FinancialAccountJpaEntity e) {
    return new FinancialAccountSummary(
        e.id(),
        e.companyId(),
        e.branchId(),
        e.type(),
        e.partnerId(),
        e.categoryId(),
        e.costCenterId(),
        e.issueDate(),
        e.totalAmount(),
        e.status());
  }
}
