/*
 * TxnFailureRollback.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

/**
 * Represents the rollback outcome of a data connection during transaction failure processing.
 *
 * <p>This class encapsulates the state of the rollback (via {@link TxnFailureRollbackState}) and
 * any error ({@link Err}) encountered when attempting to roll back the connection.
 */
public class TxnFailureRollback {

  /** The transaction failure rollback state indicating the outcome of the rollback. */
  public final TxnFailureRollbackState state;

  /** The error instance associated with a rollback failure, or {@code null} if none. */
  public final Err err;

  /**
   * Constructs a new {@code TxnFailureRollback} with the specified rollback state and error.
   *
   * @param state the transaction failure rollback state
   * @param err the error associated with the rollback failure, or {@code null}
   */
  public TxnFailureRollback(TxnFailureRollbackState state, Err err) {
    this.state = state;
    this.err = err;
  }

  /**
   * Returns a string representation of this transaction failure rollback outcome.
   *
   * @return a formatted string containing the rollback state and error details
   */
  public String toString() {
    var buf = new StringBuilder("{");
    buf.append("State:").append(this.state);
    buf.append(" ");
    buf.append("Err:").append(this.err);
    buf.append("}");
    return buf.toString();
  }
}
