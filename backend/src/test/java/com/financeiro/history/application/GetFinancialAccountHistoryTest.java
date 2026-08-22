package com.financeiro.history.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.financeiro.financialaccount.application.FinancialAccountNotFoundException;
import com.financeiro.financialaccount.application.FinancialAccountRepository;
import com.financeiro.financialaccount.domain.FinancialAccount;
import com.financeiro.financialaccount.domain.FinancialAccountStatus;
import com.financeiro.financialaccount.domain.FinancialAccountType;
import com.financeiro.financialaccount.domain.Installment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetFinancialAccountHistoryTest {
  private final FinancialAccountRepository accounts = mock(FinancialAccountRepository.class);
  private final FinancialAccountTimelineRepository timeline =
      mock(FinancialAccountTimelineRepository.class);
  private final GetFinancialAccountHistory useCase =
      new GetFinancialAccountHistory(accounts, timeline);

  @Test
  void returnsTimelineForScopedExistingAccount() {
    when(accounts.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(account()));
    var entries =
        List.of(
            new TimelineEntry(
                TimelineEntryType.CREATED,
                Instant.parse("2026-08-22T10:00:00Z"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
    when(timeline.findTimelineByFinancialAccountId(10L)).thenReturn(entries);

    assertThat(useCase.execute(1L, 10L)).isEqualTo(entries);
  }

  @Test
  void rejectsAccountOutsideCompanyScopeWithoutTouchingTimeline() {
    when(accounts.findByCompanyIdAndId(2L, 10L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(2L, 10L))
        .isInstanceOf(FinancialAccountNotFoundException.class);

    verifyNoInteractions(timeline);
  }

  private static FinancialAccount account() {
    return FinancialAccount.rehydrate(
        10L,
        1L,
        2L,
        FinancialAccountType.PAYABLE,
        3L,
        4L,
        null,
        LocalDate.of(2026, 8, 22),
        BigDecimal.TEN,
        FinancialAccountStatus.DRAFT,
        List.of(Installment.rehydrate(20L, 1, LocalDate.of(2026, 9, 1), BigDecimal.TEN)));
  }
}
