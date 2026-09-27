/*
 * TxnFailureRecovery.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

public enum TxnFailureRecovery {
  NoActionRequired,
  RerunLogicAndCommit,
  ResolveCauseThenRerunLogicAndCommit,
  ResolveCauseThenRerunPostCommit,
  ResolveCauseAndInconsistency,
  InvestigateBecauseImpossible,
  ManualRollbackRequired,
}
