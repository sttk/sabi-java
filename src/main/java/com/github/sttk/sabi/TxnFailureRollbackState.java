/*
 * TxnFailureRollbackState.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

/** Defines the states representing outcomes of transaction rollback attempts. */
public enum TxnFailureRollbackState {

  /** Indicates that the connection was successfully rolled back without error. */
  NoneByRolledBack,

  /** Indicates that no rollback was performed on the connection. */
  NoneByNotRolledBack,

  /** Indicates that an error occurred while attempting to roll back the connection. */
  RollbackFailure,
}
