package com.financeiro.paymentmethod.domain;

public final class PaymentMethod {
  private final Long id;
  private final Long companyId;
  private final String name;
  private boolean active;

  private PaymentMethod(Long id, Long companyId, String name, boolean active) {
    if (id != null && id <= 0) throw new IllegalArgumentException("id must be positive");
    if (companyId == null || companyId <= 0)
      throw new IllegalArgumentException("companyId must be positive");
    if (name == null) throw new InvalidPaymentMethodNameException();
    String normalized = name.strip();
    if (normalized.isBlank() || normalized.length() > 200)
      throw new InvalidPaymentMethodNameException();
    this.id = id;
    this.companyId = companyId;
    this.name = normalized;
    this.active = active;
  }

  public static PaymentMethod create(Long companyId, String name) {
    return new PaymentMethod(null, companyId, name, true);
  }

  public static PaymentMethod rehydrate(Long id, Long companyId, String name, boolean active) {
    if (id == null) throw new IllegalArgumentException("id must be present when rehydrating");
    return new PaymentMethod(id, companyId, name, active);
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
