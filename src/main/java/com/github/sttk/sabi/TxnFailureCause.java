/*
 * TxnFailureCause.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

/**
 * Represents the cause of a transaction failure, containing state information and the associated
 * error.
 *
 * <p>This class encapsulates the phase or status in which a transaction failure occurred (via
 * {@link TxnFailureCauseState}) and the specific {@link Err} instance detailing the failure.
 *
 * @since 1.0
 */
public class TxnFailureCause {

  /** The transaction failure cause state indicating the phase or status of the failure. */
  public final TxnFailureCauseState state;

  /** The error instance associated with the transaction failure, or {@code null} if none. */
  public final Err err;

  /**
   * Constructs a new {@code TxnFailureCause} with the specified failure state and error.
   *
   * @param state the transaction failure cause state
   * @param err the error associated with the failure
   */
  public TxnFailureCause(TxnFailureCauseState state, Err err) {
    this.state = state;
    this.err = err;
  }

  /**
   * Returns a string representation of this transaction failure cause.
   *
   * @return a formatted string containing the state and error details
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
