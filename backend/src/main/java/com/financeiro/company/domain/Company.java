package com.financeiro.company.domain;

public record Company(Long id, String name) {
    public Company {
        if (id != null && id <= 0) throw new IllegalArgumentException("id must be positive");
        name = normalizeName(name);
    }

    public static Company create(String name) { return new Company(null, name); }
    public static Company rehydrate(Long id, String name) { return new Company(id, name); }

    private static String normalizeName(String value) {
        if (value == null) throw new InvalidNameException("name is required");
        String trimmed = value.strip();
        if (trimmed.isBlank()) throw new InvalidNameException("name must not be blank");
        if (trimmed.length() > 200) throw new InvalidNameException("name must have at most 200 characters");
        return trimmed;
    }
}
