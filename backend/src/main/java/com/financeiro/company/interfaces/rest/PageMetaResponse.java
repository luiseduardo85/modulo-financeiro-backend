package com.financeiro.company.interfaces.rest;

public record PageMetaResponse(int page, int size, long totalElements, int totalPages) {}
