/*
 * TxnFailureRollbackState.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

/** Defines the rollback states of a data connection following a transaction failure. */
public enum TxnFailureRollbackState {

  /** Indicates that the connection was rolled back without error. */
  NoneByRolledBack,

  /**
   * Indicates that rollback was not performed (for example, because the connection had already
   * committed).
   */
  NoneByNotRolledBack,

  /** Indicates that rolling back the connection failed. */
  RollbackFailure,
}
