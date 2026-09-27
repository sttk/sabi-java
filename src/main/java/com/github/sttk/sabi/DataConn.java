/*
 * DataConn.java
 * Copyright (C) 2022-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.List;

public interface DataConn {
  public record FailToPreCommitDataConn(List<ErrEntry> errors) {}

  public record FailToCommitDataConn(List<ErrEntry> errors) {}

  public record FailToPostCommitDataConn(List<ErrEntry> errors) {}

  //

  default void preCommit(AsyncGroup ag) throws Err {}

  void commit(AsyncGroup ag) throws Err;

  default void postCommit(AsyncGroup ag) throws Err {}

  boolean isCommitted();

  void rollback(AsyncGroup ag) throws Err;

  default void onTxnFailure(AsyncGroup ag, List<TxnFailureReport> reports) {}

  void close();
}
