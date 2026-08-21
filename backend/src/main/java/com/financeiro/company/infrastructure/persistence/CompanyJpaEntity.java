package com.financeiro.company.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "`company`")
class CompanyJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`id`", nullable = false)
    private Long id;
    @Column(name = "`name`", nullable = false, length = 200)
    private String name;
    protected CompanyJpaEntity() {}
    CompanyJpaEntity(String name) { this.name = name; }
    Long id() { return id; }
    String name() { return name; }
}
