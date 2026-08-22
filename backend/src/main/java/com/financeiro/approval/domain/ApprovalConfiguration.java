package com.financeiro.approval.domain;

import com.financeiro.financialaccount.domain.FinancialAccountType;

public final class ApprovalConfiguration {
  private final Long id;
  private final Long companyId;
  private final Long branchId;
  private final FinancialAccountType financialAccountType;
  private final boolean approvalRequired;

  private ApprovalConfiguration(
      Long id,
      Long companyId,
      Long branchId,
      FinancialAccountType financialAccountType,
      boolean approvalRequired,
      boolean rehydrating) {
    if (rehydrating && (id == null || id <= 0)) invalid("id must be positive");
    if (!rehydrating && id != null) invalid("id must be absent when creating");
    if (companyId == null || companyId <= 0) invalid("companyId must be positive");
    if (branchId != null && branchId <= 0) invalid("branchId must be positive when present");
    if (financialAccountType == null) invalid("financialAccountType is required");
    this.id = id;
    this.companyId = companyId;
    this.branchId = branchId;
    this.financialAccountType = financialAccountType;
    this.approvalRequired = approvalRequired;
  }

  public static ApprovalConfiguration create(
      Long companyId,
      Long branchId,
      FinancialAccountType financialAccountType,
      boolean approvalRequired) {
    return new ApprovalConfiguration(
        null, companyId, branchId, financialAccountType, approvalRequired, false);
  }

  public static ApprovalConfiguration rehydrate(
      Long id,
      Long companyId,
      Long branchId,
      FinancialAccountType financialAccountType,
      boolean approvalRequired) {
    return new ApprovalConfiguration(
        id, companyId, branchId, financialAccountType, approvalRequired, true);
  }

  private static void invalid(String message) {
    throw new InvalidApprovalConfigurationException(message);
  }

  public Long id() {
    return id;
  }

  public Long companyId() {
    return companyId;
  }

  public Long branchId() {
    return branchId;
  }

  public FinancialAccountType financialAccountType() {
    return financialAccountType;
  }

  public boolean approvalRequired() {
    return approvalRequired;
  }
}
