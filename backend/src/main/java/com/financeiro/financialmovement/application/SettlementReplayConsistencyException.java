package com.financeiro.financialmovement.application;

public final class SettlementReplayConsistencyException extends RuntimeException {
  public SettlementReplayConsistencyException() {
    super("The completed settlement result could not be reloaded.");
  }
}
