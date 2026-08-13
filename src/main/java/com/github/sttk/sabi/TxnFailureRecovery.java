/*
 * TxnFailureRecovery.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

/**
 * Defines recommended recovery actions to handle transaction failures.
 *
 * @since 1.0
 */
public enum TxnFailureRecovery {

  /** Indicates that no recovery action is required. */
  NoActionRequired,

  /** Indicates that the logic should be rerun and committed again without further changes. */
  RerunLogicAndCommit,

  /** Indicates that the root cause must be resolved before rerunning logic and committing. */
  ResolveCauseThenRerunLogicAndCommit,

  /** Indicates that the root cause must be resolved before rerunning the post-commit phase. */
  ResolveCauseThenRerunPostCommit,

  /** Indicates that both the root cause and any resulting data inconsistency must be resolved. */
  ResolveCauseAndInconsistency,

  /** Indicates an impossible state occurred that requires investigation. */
  InvestigateBecauseImpossible,

  /** Indicates that manual intervention is required to roll back changes. */
  ManualRollbackRequired,
}
