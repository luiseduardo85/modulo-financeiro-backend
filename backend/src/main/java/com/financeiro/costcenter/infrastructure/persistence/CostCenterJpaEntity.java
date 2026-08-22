package com.financeiro.costcenter.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "`costCenter`")
class CostCenterJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`id`", nullable = false)
  private Long id;

  @Column(name = "`companyId`", nullable = false)
  private Long companyId;

  @Column(name = "`name`", nullable = false, length = 200)
  private String name;

  @Column(name = "`active`", nullable = false)
  private boolean active;

  protected CostCenterJpaEntity() {}

  CostCenterJpaEntity(Long id, Long companyId, String name, boolean active) {
    this.id = id;
    this.companyId = companyId;
    this.name = name;
    this.active = active;
  }

  Long id() {
    return id;
  }

  Long companyId() {
    return companyId;
  }

  String name() {
    return name;
  }

  boolean active() {
    return active;
  }
}
