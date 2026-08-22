package com.financeiro.company.application;

import com.financeiro.company.domain.Branch;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetBranch {
  private final BranchRepository repository;

  public GetBranch(BranchRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Branch execute(Long companyId, Long id) {
    return repository
        .findByCompanyIdAndId(companyId, id)
        .orElseThrow(() -> new BranchNotFoundException(id));
  }
}
