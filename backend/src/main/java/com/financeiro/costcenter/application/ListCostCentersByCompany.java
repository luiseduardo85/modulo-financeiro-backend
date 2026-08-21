package com.financeiro.costcenter.application;

import com.financeiro.company.application.*;
import com.financeiro.costcenter.domain.CostCenter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListCostCentersByCompany {
  private final CompanyRepository companies;
  private final CostCenterRepository costCenters;

  public ListCostCentersByCompany(CompanyRepository companies, CostCenterRepository costCenters) {
    this.companies = companies;
    this.costCenters = costCenters;
  }

  @Transactional(readOnly = true)
  public PageResult<CostCenter> execute(Long companyId, PageQuery query) {
    companies.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    return costCenters.findPageByCompanyId(companyId, query);
  }
}
