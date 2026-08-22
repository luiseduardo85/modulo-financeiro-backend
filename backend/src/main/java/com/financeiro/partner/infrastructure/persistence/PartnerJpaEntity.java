package com.financeiro.partner.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "`partner`")
class PartnerJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`id`", nullable = false)
  private Long id;

  @Column(name = "`name`", nullable = false, length = 200)
  private String name;

  @Column(name = "`document`", nullable = false, length = 14)
  private String document;

  @Column(name = "`customer`", nullable = false)
  private boolean customer;

  @Column(name = "`supplier`", nullable = false)
  private boolean supplier;

  @Column(name = "`active`", nullable = false)
  private boolean active;

  protected PartnerJpaEntity() {}

  PartnerJpaEntity(
      Long id, String name, String document, boolean customer, boolean supplier, boolean active) {
    this.id = id;
    this.name = name;
    this.document = document;
    this.customer = customer;
    this.supplier = supplier;
    this.active = active;
  }

  Long id() {
    return id;
  }

  String name() {
    return name;
  }

  String document() {
    return document;
  }

  boolean customer() {
    return customer;
  }

  boolean supplier() {
    return supplier;
  }

  boolean active() {
    return active;
  }
}
