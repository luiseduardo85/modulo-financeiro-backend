package com.financeiro.category.infrastructure.persistence;

import com.financeiro.category.application.CategoryRepository;
import com.financeiro.category.domain.Category;
import com.financeiro.company.application.PageQuery;
import com.financeiro.company.application.PageResult;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaCategoryRepositoryAdapter implements CategoryRepository {
  private final SpringDataCategoryRepository repository;

  public JpaCategoryRepositoryAdapter(SpringDataCategoryRepository repository) {
    this.repository = repository;
  }

  @Override
  public Category save(Category value) {
    return toDomain(
        repository.save(
            new CategoryJpaEntity(value.id(), value.companyId(), value.name(), value.active())));
  }

  @Override
  public Optional<Category> findByCompanyIdAndId(Long companyId, Long id) {
    return repository
        .findByCompanyIdAndId(companyId, id)
        .map(JpaCategoryRepositoryAdapter::toDomain);
  }

  @Override
  public PageResult<Category> findPageByCompanyId(Long companyId, PageQuery query) {
    var page =
        repository.findByCompanyId(
            companyId, PageRequest.of(query.page(), query.size(), sort(query)));
    return new PageResult<>(
        page.map(JpaCategoryRepositoryAdapter::toDomain).getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }

  private static Category toDomain(CategoryJpaEntity e) {
    return Category.rehydrate(e.id(), e.companyId(), e.name(), e.active());
  }

  private static Sort sort(PageQuery q) {
    String property = q.sortField() == PageQuery.SortField.ID ? "id" : "name";
    Sort.Direction direction =
        q.direction() == PageQuery.SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
    Sort result = Sort.by(direction, property);
    return property.equals("name") ? result.and(Sort.by(direction, "id")) : result;
  }
}
