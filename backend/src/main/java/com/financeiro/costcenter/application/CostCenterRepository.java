package com.financeiro.costcenter.application;

import com.financeiro.company.application.PageQuery;
import com.financeiro.company.application.PageResult;
import com.financeiro.costcenter.domain.CostCenter;
import java.util.Optional;

public interface CostCenterRepository {
  CostCenter save(CostCenter costCenter);

  Optional<CostCenter> findByCompanyIdAndId(Long companyId, Long id);

  PageResult<CostCenter> findPageByCompanyId(Long companyId, PageQuery query);
}
