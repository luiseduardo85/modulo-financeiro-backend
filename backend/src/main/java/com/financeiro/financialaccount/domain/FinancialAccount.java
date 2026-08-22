package com.financeiro.financialaccount.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

public final class FinancialAccount {
  private final Long id;
  private final Long companyId;
  private final Long branchId;
  private final FinancialAccountType type;
  private final Long partnerId;
  private final Long categoryId;
  private final Long costCenterId;
  private final LocalDate issueDate;
  private final BigDecimal totalAmount;
  private final FinancialAccountStatus status;
  private final List<Installment> installments;

  private FinancialAccount(
      Long id,
      Long companyId,
      Long branchId,
      FinancialAccountType type,
      Long partnerId,
      Long categoryId,
      Long costCenterId,
      LocalDate issueDate,
      BigDecimal totalAmount,
      FinancialAccountStatus status,
      List<Installment> installments,
      boolean rehydrating) {
    if (rehydrating && (id == null || id <= 0)) invalid("id is required when rehydrating");
    if (!rehydrating && id != null) invalid("id must be absent when creating");
    positive(companyId, "companyId");
    positive(branchId, "branchId");
    positive(partnerId, "partnerId");
    positive(categoryId, "categoryId");
    if (costCenterId != null) positive(costCenterId, "costCenterId");
    if (type == null) invalid("type is required");
    if (issueDate == null) invalid("issueDate is required");
    if (status == null) invalid("status is required");
    validateTotal(totalAmount);
    if (installments == null || installments.isEmpty()) invalid("installments are required");
    if (installments.stream().anyMatch(java.util.Objects::isNull))
      invalid("installments must not contain null");
    if (!rehydrating && installments.stream().anyMatch(item -> item.id() != null))
      invalid("installment id must be absent when creating");
    if (rehydrating && installments.stream().anyMatch(item -> item.id() == null))
      invalid("installment id is required when rehydrating");
    var numbers = new HashSet<Integer>();
    if (installments.stream().anyMatch(item -> !numbers.add(item.installmentNumber())))
      invalid("installmentNumber must be unique");
    BigDecimal sum =
        installments.stream().map(Installment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (sum.compareTo(totalAmount) != 0) invalid("installment total must equal totalAmount");

    this.id = id;
    this.companyId = companyId;
    this.branchId = branchId;
    this.type = type;
    this.partnerId = partnerId;
    this.categoryId = categoryId;
    this.costCenterId = costCenterId;
    this.issueDate = issueDate;
    this.totalAmount = totalAmount;
    this.status = status;
    this.installments = List.copyOf(installments);
  }

  public static FinancialAccount create(
      Long companyId,
      Long branchId,
      FinancialAccountType type,
      Long partnerId,
      Long categoryId,
      Long costCenterId,
      LocalDate issueDate,
      BigDecimal totalAmount,
      List<Installment> installments) {
    return new FinancialAccount(
        null,
        companyId,
        branchId,
        type,
        partnerId,
        categoryId,
        costCenterId,
        issueDate,
        totalAmount,
        FinancialAccountStatus.DRAFT,
        installments,
        false);
  }

  public static FinancialAccount rehydrate(
      Long id,
      Long companyId,
      Long branchId,
      FinancialAccountType type,
      Long partnerId,
      Long categoryId,
      Long costCenterId,
      LocalDate issueDate,
      BigDecimal totalAmount,
      FinancialAccountStatus status,
      List<Installment> installments) {
    return new FinancialAccount(
        id,
        companyId,
        branchId,
        type,
        partnerId,
        categoryId,
        costCenterId,
        issueDate,
        totalAmount,
        status,
        installments,
        true);
  }

  private static void validateTotal(BigDecimal value) {
    try {
      Installment.validateMoney(value);
    } catch (InvalidInstallmentException exception) {
      throw new InvalidFinancialAccountException(
          exception.getMessage().replace("amount", "totalAmount"));
    }
  }

  private static void positive(Long value, String field) {
    if (value == null || value <= 0) invalid(field + " must be positive");
  }

  private static void invalid(String message) {
    throw new InvalidFinancialAccountException(message);
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

  public FinancialAccountType type() {
    return type;
  }

  public Long partnerId() {
    return partnerId;
  }

  public Long categoryId() {
    return categoryId;
  }

  public Long costCenterId() {
    return costCenterId;
  }

  public LocalDate issueDate() {
    return issueDate;
  }

  public BigDecimal totalAmount() {
    return totalAmount;
  }

  public FinancialAccountStatus status() {
    return status;
  }

  public List<Installment> installments() {
    return installments;
  }
}
