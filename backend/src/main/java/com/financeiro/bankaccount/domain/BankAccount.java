package com.financeiro.bankaccount.domain;

public final class BankAccount {
  private final Long id;
  private final Long companyId;
  private final Long branchId;
  private final String name;
  private boolean active;

  private BankAccount(Long id, Long companyId, Long branchId, String name, boolean active) {
    if (id != null && id <= 0) throw new IllegalArgumentException("id must be positive");
    if (companyId == null || companyId <= 0)
      throw new IllegalArgumentException("companyId must be positive");
    if (branchId != null && branchId <= 0)
      throw new IllegalArgumentException("branchId must be positive");
    if (name == null) throw new InvalidBankAccountNameException();
    String normalized = name.strip();
    if (normalized.isBlank() || normalized.length() > 200)
      throw new InvalidBankAccountNameException();
    this.id = id;
    this.companyId = companyId;
    this.branchId = branchId;
    this.name = normalized;
    this.active = active;
  }

  public static BankAccount create(Long companyId, Long branchId, String name) {
    return new BankAccount(null, companyId, branchId, name, true);
  }

  public static BankAccount rehydrate(
      Long id, Long companyId, Long branchId, String name, boolean active) {
    if (id == null) throw new IllegalArgumentException("id must be present when rehydrating");
    return new BankAccount(id, companyId, branchId, name, active);
  }

  public void deactivate() {
    active = false;
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

  public String name() {
    return name;
  }

  public boolean active() {
    return active;
  }
}
