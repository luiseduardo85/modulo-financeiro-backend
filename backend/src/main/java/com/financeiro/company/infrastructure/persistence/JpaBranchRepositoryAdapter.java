package com.financeiro.company.infrastructure.persistence;

import com.financeiro.company.application.BranchRepository;
import com.financeiro.company.application.PageQuery;
import com.financeiro.company.application.PageResult;
import com.financeiro.company.domain.Branch;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaBranchRepositoryAdapter implements BranchRepository {
  private final SpringDataBranchRepository repository;

  public JpaBranchRepositoryAdapter(SpringDataBranchRepository repository) {
    this.repository = repository;
  }

  private static Branch toDomain(BranchJpaEntity entity) {
    return Branch.rehydrate(entity.id(), entity.companyId(), entity.name());
  }

  @Override
  public Branch save(Branch branch) {
    return toDomain(repository.save(new BranchJpaEntity(branch.companyId(), branch.name())));
  }

  @Override
  public Optional<Branch> findByCompanyIdAndId(Long companyId, Long id) {
    return repository.findByCompanyIdAndId(companyId, id).map(JpaBranchRepositoryAdapter::toDomain);
  }

  @Override
  public PageResult<Branch> findPageByCompanyId(Long companyId, PageQuery query) {
    var page = repository.findByCompanyId(companyId, JpaCompanyRepositoryAdapter.pageable(query));
    return new PageResult<>(
        page.map(JpaBranchRepositoryAdapter::toDomain).getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
