package com.financeiro.company.infrastructure.persistence;

import com.financeiro.company.application.*;
import com.financeiro.company.domain.Company;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository @Profile("!test")
public class JpaCompanyRepositoryAdapter implements CompanyRepository {
    private final SpringDataCompanyRepository repository;
    public JpaCompanyRepositoryAdapter(SpringDataCompanyRepository repository) { this.repository = repository; }
    @Override public Company save(Company company) { return toDomain(repository.save(new CompanyJpaEntity(company.name()))); }
    @Override public Optional<Company> findById(Long id) { return repository.findById(id).map(JpaCompanyRepositoryAdapter::toDomain); }
    @Override public PageResult<Company> findPage(PageQuery query) {
        var page = repository.findAll(pageable(query));
        return new PageResult<>(page.map(JpaCompanyRepositoryAdapter::toDomain).getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
    private static Company toDomain(CompanyJpaEntity entity) { return Company.rehydrate(entity.id(), entity.name()); }
    static PageRequest pageable(PageQuery query) {
        String property = query.sortField() == PageQuery.SortField.ID ? "id" : "name";
        Sort.Direction direction = query.direction() == PageQuery.SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(query.page(), query.size(), Sort.by(direction, property));
    }
}
