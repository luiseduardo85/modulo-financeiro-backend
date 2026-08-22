package com.financeiro.cashflow.application;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CashFlowQueryTest {
  private static final LocalDate START = LocalDate.of(2026, 8, 1);
  private static final LocalDate END = LocalDate.of(2026, 8, 31);

  @Test
  void acceptsValidQueryWithAndWithoutBranch() {
    assertThatCode(() -> new CashFlowQuery(1L, null, START, END)).doesNotThrowAnyException();
    assertThatCode(() -> new CashFlowQuery(1L, 2L, START, END)).doesNotThrowAnyException();
  }

  @Test
  void rejectsNonPositiveCompanyId() {
    assertThatThrownBy(() -> new CashFlowQuery(null, null, START, END))
        .isInstanceOf(InvalidCashFlowQueryException.class);
    assertThatThrownBy(() -> new CashFlowQuery(0L, null, START, END))
        .isInstanceOf(InvalidCashFlowQueryException.class);
    assertThatThrownBy(() -> new CashFlowQuery(-1L, null, START, END))
        .isInstanceOf(InvalidCashFlowQueryException.class);
  }

  @Test
  void rejectsNonPositiveBranchIdButAllowsAbsentBranch() {
    assertThatThrownBy(() -> new CashFlowQuery(1L, 0L, START, END))
        .isInstanceOf(InvalidCashFlowQueryException.class);
    assertThatThrownBy(() -> new CashFlowQuery(1L, -1L, START, END))
        .isInstanceOf(InvalidCashFlowQueryException.class);
  }

  @Test
  void rejectsMissingDates() {
    assertThatThrownBy(() -> new CashFlowQuery(1L, null, null, END))
        .isInstanceOf(InvalidCashFlowQueryException.class);
    assertThatThrownBy(() -> new CashFlowQuery(1L, null, START, null))
        .isInstanceOf(InvalidCashFlowQueryException.class);
  }

  @Test
  void rejectsStartDateAfterEndDate() {
    assertThatThrownBy(() -> new CashFlowQuery(1L, null, END, START))
        .isInstanceOf(InvalidCashFlowQueryException.class);
  }

  @Test
  void acceptsSameStartAndEndDate() {
    assertThatCode(() -> new CashFlowQuery(1L, null, START, START)).doesNotThrowAnyException();
  }

  @Test
  void rejectsRangeExceedingOneYear() {
    LocalDate start = LocalDate.of(2026, 1, 1);
    assertThatCode(() -> new CashFlowQuery(1L, null, start, start.plusDays(366)))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> new CashFlowQuery(1L, null, start, start.plusDays(367)))
        .isInstanceOf(InvalidCashFlowQueryException.class);
  }
}
