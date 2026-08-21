package com.financeiro.company.domain;

public record Branch(Long id, Long companyId, String name) {
    public Branch {
        if (id != null && id <= 0) throw new IllegalArgumentException("id must be positive");
        if (companyId == null || companyId <= 0) throw new IllegalArgumentException("companyId must be positive");
        if (name == null) throw new InvalidNameException("name is required");
        name = name.strip();
        if (name.isBlank()) throw new InvalidNameException("name must not be blank");
        if (name.length() > 200) throw new InvalidNameException("name must have at most 200 characters");
    }

    public static Branch create(Long companyId, String name) { return new Branch(null, companyId, name); }
    public static Branch rehydrate(Long id, Long companyId, String name) { return new Branch(id, companyId, name); }
}
