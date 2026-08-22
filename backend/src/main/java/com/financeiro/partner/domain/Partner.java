package com.financeiro.partner.domain;

import java.util.Set;

public final class Partner {
  private final Long id;
  private final String name;
  private final Document document;
  private final Set<PartnerRole> roles;
  private boolean active;

  private Partner(Long id, String name, Document document, Set<PartnerRole> roles, boolean active) {
    if (id != null && id <= 0) throw new IllegalArgumentException("id must be positive");
    if (name == null) throw new InvalidPartnerNameException();
    String normalized = name.strip();
    if (normalized.isBlank() || normalized.length() > 200) throw new InvalidPartnerNameException();
    if (document == null) throw new InvalidPartnerDocumentException();
    if (roles == null || roles.isEmpty() || roles.stream().anyMatch(java.util.Objects::isNull))
      throw new InvalidPartnerRolesException();
    this.id = id;
    this.name = normalized;
    this.document = document;
    this.roles = Set.copyOf(roles);
    this.active = active;
  }

  public static Partner create(String name, Document document, Set<PartnerRole> roles) {
    return new Partner(null, name, document, roles, true);
  }

  public static Partner rehydrate(
      Long id, String name, Document document, Set<PartnerRole> roles, boolean active) {
    if (id == null) {
      throw new IllegalArgumentException("id must be present when rehydrating");
    }
    return new Partner(id, name, document, roles, active);
  }

  public void deactivate() {
    active = false;
  }

  public Long id() {
    return id;
  }

  public String name() {
    return name;
  }

  public Document document() {
    return document;
  }

  public Set<PartnerRole> roles() {
    return roles;
  }

  public boolean active() {
    return active;
  }
}
