package com.financeiro.costcenter.application;

import com.financeiro.costcenter.domain.CostCenter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeactivateCostCenter {
  private final CostCenterRepository costCenters;

  public DeactivateCostCenter(CostCenterRepository costCenters) {
    this.costCenters = costCenters;
  }

  @Transactional
  public CostCenter execute(Long companyId, Long costCenterId) {
    CostCenter value =
        costCenters
            .findByCompanyIdAndId(companyId, costCenterId)
            .orElseThrow(() -> new CostCenterNotFoundException(companyId, costCenterId));
    value.deactivate();
    return costCenters.save(value);
  }
}
