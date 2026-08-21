package com.financeiro.costcenter.infrastructure.persistence;

import com.financeiro.company.application.PageQuery;
import com.financeiro.company.application.PageResult;
import com.financeiro.costcenter.application.CostCenterRepository;
import com.financeiro.costcenter.domain.CostCenter;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaCostCenterRepositoryAdapter implements CostCenterRepository {
  private final SpringDataCostCenterRepository repository;

  public JpaCostCenterRepositoryAdapter(SpringDataCostCenterRepository repository) {
    this.repository = repository;
  }

  @Override
  public CostCenter save(CostCenter value) {
    return toDomain(
        repository.save(
            new CostCenterJpaEntity(value.id(), value.companyId(), value.name(), value.active())));
  }

  @Override
  public Optional<CostCenter> findByCompanyIdAndId(Long companyId, Long id) {
    return repository
        .findByCompanyIdAndId(companyId, id)
        .map(JpaCostCenterRepositoryAdapter::toDomain);
  }

  @Override
  public PageResult<CostCenter> findPageByCompanyId(Long companyId, PageQuery query) {
    var page =
        repository.findByCompanyId(
            companyId, PageRequest.of(query.page(), query.size(), sort(query)));
    return new PageResult<>(
        page.map(JpaCostCenterRepositoryAdapter::toDomain).getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }

  private static CostCenter toDomain(CostCenterJpaEntity e) {
    return CostCenter.rehydrate(e.id(), e.companyId(), e.name(), e.active());
  }

  private static Sort sort(PageQuery q) {
    String property = q.sortField() == PageQuery.SortField.ID ? "id" : "name";
    Sort.Direction direction =
        q.direction() == PageQuery.SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
    Sort result = Sort.by(direction, property);
    return property.equals("name") ? result.and(Sort.by(direction, "id")) : result;
  }
}
