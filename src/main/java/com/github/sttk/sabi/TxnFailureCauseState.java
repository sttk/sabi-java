/*
 * TxnFailureCause.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

public enum TxnFailureCauseState {
  NoneByCommitted,
  NoneByUncommitted,
  LogicFailure,
  CommitFailure,
  PostCommitFailure,
}
