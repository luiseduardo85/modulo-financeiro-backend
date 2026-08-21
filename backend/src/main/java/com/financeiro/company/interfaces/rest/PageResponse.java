package com.financeiro.company.interfaces.rest;

import java.util.List;

public record PageResponse<T>(List<T> data, PageMetaResponse meta) {}
