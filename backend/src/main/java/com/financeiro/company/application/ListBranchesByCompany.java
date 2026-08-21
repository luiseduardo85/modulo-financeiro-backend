package com.financeiro.company.application;

import com.financeiro.company.domain.Branch;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListBranchesByCompany {
    private final CompanyRepository companies; private final BranchRepository branches;
    public ListBranchesByCompany(CompanyRepository companies, BranchRepository branches) { this.companies = companies; this.branches = branches; }
    @Transactional(readOnly = true) public PageResult<Branch> execute(Long companyId, PageQuery query) {
        companies.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
        return branches.findPageByCompanyId(companyId, query);
    }
}
