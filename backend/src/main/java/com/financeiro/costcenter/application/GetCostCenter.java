package com.financeiro.costcenter.application;

import com.financeiro.costcenter.domain.CostCenter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCostCenter {
  private final CostCenterRepository costCenters;

  public GetCostCenter(CostCenterRepository costCenters) {
    this.costCenters = costCenters;
  }

  @Transactional(readOnly = true)
  public CostCenter execute(Long companyId, Long costCenterId) {
    return costCenters
        .findByCompanyIdAndId(companyId, costCenterId)
        .orElseThrow(() -> new CostCenterNotFoundException(companyId, costCenterId));
  }
}
