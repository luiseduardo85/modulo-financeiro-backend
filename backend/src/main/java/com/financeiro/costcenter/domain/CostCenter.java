package com.financeiro.costcenter.domain;

public final class CostCenter {
  private final Long id;
  private final Long companyId;
  private final String name;
  private boolean active;

  private CostCenter(Long id, Long companyId, String name, boolean active) {
    if (id != null && id <= 0) throw new IllegalArgumentException("id must be positive");
    if (companyId == null || companyId <= 0)
      throw new IllegalArgumentException("companyId must be positive");
    if (name == null) throw new InvalidCostCenterNameException();
    String normalized = name.strip();
    if (normalized.isBlank() || normalized.length() > 200)
      throw new InvalidCostCenterNameException();
    this.id = id;
    this.companyId = companyId;
    this.name = normalized;
    this.active = active;
  }

  public static CostCenter create(Long companyId, String name) {
    return new CostCenter(null, companyId, name, true);
  }

  public static CostCenter rehydrate(Long id, Long companyId, String name, boolean active) {
    if (id == null) throw new IllegalArgumentException("id must be present when rehydrating");
    return new CostCenter(id, companyId, name, active);
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

  public String name() {
    return name;
  }

  public boolean active() {
    return active;
  }
}
