package com.financeiro.company.application;

import com.financeiro.company.domain.Branch;
import java.util.Optional;

public interface BranchRepository {
    Branch save(Branch branch);
    Optional<Branch> findByCompanyIdAndId(Long companyId, Long id);
    PageResult<Branch> findPageByCompanyId(Long companyId, PageQuery query);
}
