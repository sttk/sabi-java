/*
 * TxnFailureCause.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

public final class TxnFailureCause {

  public final TxnFailureCauseState state;
  public final Err err;

  public TxnFailureCause(TxnFailureCauseState state, Err err) {
    this.state = state;
    this.err = err;
  }

  public String toString() {
    var buf = new StringBuilder("{");
    buf.append("State:").append(this.state);
    buf.append(" ");
    buf.append("Err:").append(this.err);
    buf.append("}");
    return buf.toString();
  }
}
