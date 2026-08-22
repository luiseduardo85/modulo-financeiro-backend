package com.financeiro.bankaccount.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "`bankAccount`")
class BankAccountJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`id`", nullable = false)
  private Long id;

  @Column(name = "`companyId`", nullable = false)
  private Long companyId;

  @Column(name = "`branchId`")
  private Long branchId;

  @Column(name = "`name`", nullable = false, length = 200)
  private String name;

  @Column(name = "`active`", nullable = false)
  private boolean active;

  protected BankAccountJpaEntity() {}

  BankAccountJpaEntity(Long id, Long companyId, Long branchId, String name, boolean active) {
    this.id = id;
    this.companyId = companyId;
    this.branchId = branchId;
    this.name = name;
    this.active = active;
  }

  Long id() {
    return id;
  }

  Long companyId() {
    return companyId;
  }

  Long branchId() {
    return branchId;
  }

  String name() {
    return name;
  }

  boolean active() {
    return active;
  }
}
