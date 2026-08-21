package com.financeiro.company.interfaces.rest;

import com.financeiro.company.application.InvalidPageRequestException;
import com.financeiro.company.application.PageQuery;
import org.springframework.stereotype.Component;

@Component
public final class PageQueryParser {
    public PageQuery parse(int page, int size, String sort) {
        if (sort == null) throw new InvalidPageRequestException("sort is invalid");
        String[] parts = sort.split(",", -1);
        if (parts.length != 2) throw new InvalidPageRequestException("sort must use field,direction syntax");
        PageQuery.SortField field = switch (parts[0]) {
            case "id" -> PageQuery.SortField.ID;
            case "name" -> PageQuery.SortField.NAME;
            default -> throw new InvalidPageRequestException("sort field is invalid");
        };
        PageQuery.SortDirection direction = switch (parts[1]) {
            case "asc" -> PageQuery.SortDirection.ASC;
            case "desc" -> PageQuery.SortDirection.DESC;
            default -> throw new InvalidPageRequestException("sort direction is invalid");
        };
        return new PageQuery(page, size, field, direction);
    }
}
