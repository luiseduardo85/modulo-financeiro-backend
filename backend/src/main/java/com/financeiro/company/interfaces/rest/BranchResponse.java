package com.financeiro.company.interfaces.rest;

import com.financeiro.company.domain.Branch;

public record BranchResponse(Long id, Long companyId, String name) {
  static BranchResponse from(Branch branch) {
    return new BranchResponse(branch.id(), branch.companyId(), branch.name());
  }
}
