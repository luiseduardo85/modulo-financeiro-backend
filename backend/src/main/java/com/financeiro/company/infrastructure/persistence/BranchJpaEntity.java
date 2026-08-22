package com.financeiro.company.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "`branch`")
class BranchJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`id`", nullable = false)
  private Long id;

  @Column(name = "`companyId`", nullable = false)
  private Long companyId;

  @Column(name = "`name`", nullable = false, length = 200)
  private String name;

  protected BranchJpaEntity() {}

  BranchJpaEntity(Long companyId, String name) {
    this.companyId = companyId;
    this.name = name;
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
}
