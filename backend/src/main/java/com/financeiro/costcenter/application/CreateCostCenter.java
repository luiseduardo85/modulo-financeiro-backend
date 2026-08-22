package com.financeiro.costcenter.application;

import com.financeiro.company.application.CompanyNotFoundException;
import com.financeiro.company.application.CompanyRepository;
import com.financeiro.costcenter.domain.CostCenter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCostCenter {
  private final CompanyRepository companies;
  private final CostCenterRepository costCenters;

  public CreateCostCenter(CompanyRepository companies, CostCenterRepository costCenters) {
    this.companies = companies;
    this.costCenters = costCenters;
  }

  @Transactional
  public CostCenter execute(Long companyId, String name) {
    companies.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    return costCenters.save(CostCenter.create(companyId, name));
  }
}
