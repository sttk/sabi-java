/*
 * TxnFailureCauseState.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

/** Defines the states representing causes of transaction failures or non-failing statuses. */
public enum TxnFailureCauseState {

  /** Indicates no failure occurred on this connection and it was committed successfully. */
  NoneByCommitted,

  /** Indicates no failure occurred on this connection and it remained uncommitted. */
  NoneByUncommitted,

  /** Indicates that the transaction failed due to an error during logic execution. */
  LogicFailure,

  /** Indicates that the transaction failed during the pre-commit phase of a connection. */
  PreCommitFailure,

  /** Indicates that the transaction failed during the commit phase of a connection. */
  CommitFailure,

  /** Indicates that the transaction failed during the post-commit phase of a connection. */
  PostCommitFailure,
}
