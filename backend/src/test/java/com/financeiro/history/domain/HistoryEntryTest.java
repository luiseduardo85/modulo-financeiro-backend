package com.financeiro.history.domain;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class HistoryEntryTest {

  @Test
  void exposesOnlyFactoriesForConstruction() {
    assertThat(Arrays.stream(HistoryEntry.class.getDeclaredConstructors()))
        .allMatch(constructor -> Modifier.isPrivate(constructor.getModifiers()));
  }

  @Test
  void createdHasNoActorAndNoPersistedId() {
    var entry = HistoryEntry.created(10L);
    assertThat(entry.id()).isNull();
    assertThat(entry.financialAccountId()).isEqualTo(10L);
    assertThat(entry.type()).isEqualTo(HistoryEntryType.CREATED);
    assertThat(entry.actorId()).isNull();
  }

  @Test
  void createdRequiresPositiveFinancialAccountId() {
    for (Long id : new Long[] {null, 0L, -1L}) {
      assertThatThrownBy(() -> HistoryEntry.created(id))
          .isInstanceOf(InvalidHistoryEntryException.class);
    }
  }

  @Test
  void approvedWithoutWorkflowNormalizesActor() {
    var entry = HistoryEntry.approvedWithoutWorkflow(10L, "  requester  ");
    assertThat(entry.type()).isEqualTo(HistoryEntryType.APPROVED_WITHOUT_WORKFLOW);
    assertThat(entry.actorId()).isEqualTo("requester");
  }

  @Test
  void approvedWithoutWorkflowRejectsBlankOrOversizedActor() {
    for (String actor : new String[] {null, " ", "a".repeat(129)}) {
      assertThatThrownBy(() -> HistoryEntry.approvedWithoutWorkflow(10L, actor))
          .isInstanceOf(InvalidHistoryEntryException.class);
    }
  }

  @Test
  void rehydrateRequiresPositiveId() {
    for (Long id : new Long[] {null, 0L, -1L}) {
      assertThatThrownBy(() -> HistoryEntry.rehydrate(id, 10L, HistoryEntryType.CREATED, null))
          .isInstanceOf(InvalidHistoryEntryException.class);
    }
  }

  @Test
  void rehydrateRejectsCreatedWithActor() {
    assertThatThrownBy(() -> HistoryEntry.rehydrate(1L, 10L, HistoryEntryType.CREATED, "someone"))
        .isInstanceOf(InvalidHistoryEntryException.class);
  }

  @Test
  void rehydrateRejectsApprovedWithoutWorkflowMissingActor() {
    assertThatThrownBy(
            () -> HistoryEntry.rehydrate(1L, 10L, HistoryEntryType.APPROVED_WITHOUT_WORKFLOW, null))
        .isInstanceOf(InvalidHistoryEntryException.class);
  }

  @Test
  void rehydrateAcceptsConsistentValues() {
    var entry =
        HistoryEntry.rehydrate(1L, 10L, HistoryEntryType.APPROVED_WITHOUT_WORKFLOW, "actor");
    assertThat(entry.id()).isEqualTo(1L);
    assertThat(entry.actorId()).isEqualTo("actor");
  }
}
