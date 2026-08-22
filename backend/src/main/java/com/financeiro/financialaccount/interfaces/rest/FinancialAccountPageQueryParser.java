package com.financeiro.financialaccount.interfaces.rest;

import com.financeiro.financialaccount.application.*;
import org.springframework.stereotype.Component;

@Component
public class FinancialAccountPageQueryParser {
  public FinancialAccountPageQuery parse(int page, int size, String sort) {
    if (page < 0) throw new InvalidFinancialAccountPageException();
    if (size < 1 || size > 100) throw new InvalidFinancialAccountPageException();
    if (sort == null) throw new InvalidFinancialAccountPageException();
    var parts = sort.split(",", -1);
    if (parts.length != 2 || !parts[0].equals("id"))
      throw new InvalidFinancialAccountPageException();
    FinancialAccountPageQuery.Direction direction;
    try {
      direction =
          FinancialAccountPageQuery.Direction.valueOf(parts[1].toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new InvalidFinancialAccountPageException();
    }
    return new FinancialAccountPageQuery(page, size, direction);
  }
}
