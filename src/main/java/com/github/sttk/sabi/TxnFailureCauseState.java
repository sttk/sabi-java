/*
 * TxnFailureCause.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

/**
 * Defines the states representing causes of transaction failures or successful states.
 *
 * @since 1.0
 */
public enum TxnFailureCauseState {

  /** Indicates no failure occurred and the connection was committed successfully. */
  NoneByCommitted,

  /** Indicates no failure occurred and the connection remained uncommitted. */
  NoneByUncommitted,

  /** Indicates that the transaction failed due to an error during logic execution. */
  LogicFailure,

  /** Indicates that the transaction failed during the commit phase of a connection. */
  CommitFailure,

  /** Indicates that the transaction failed during the post-commit phase of a connection. */
  PostCommitFailure,
}
