/*
 * DataConn.java
 * Copyright (C) 2022-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.List;

/**
 * Represents a data connection participating in transaction lifecycles managed by a {@link
 * DataHub}.
 *
 * <p>Implementations encapsulate connection state and operations for external resources such as
 * relational databases, NoSQL stores, or web services. During a transaction, connections progress
 * through pre-commit, commit, and post-commit phases, and support rollback, failure reporting, and
 * abnormal termination handling when errors occur.
 */
public interface DataConn {

  /**
   * Prepares this connection for commit prior to the main commit phase.
   *
   * <p>The default implementation does nothing. Implementations can override this method to perform
   * pre-commit checks or flush pending writes.
   *
   * @param ag the asynchronous group for registering background tasks
   * @throws Err if pre-commit preparation or validation fails
   */
  default void preCommit(AsyncGroup ag) throws Err {}

  /**
   * Commits changes made through this connection.
   *
   * <p>Asynchronous background tasks may be registered with the provided {@link AsyncGroup} during
   * commit processing.
   *
   * @param ag the asynchronous group for registering background tasks
   * @throws Err if committing the transaction changes fails
   */
  void commit(AsyncGroup ag) throws Err;

  /**
   * Performs post-commit operations after all connections in a transaction have successfully
   * committed.
   *
   * <p>The default implementation does nothing. Implementations can override this method to perform
   * cleanup or trigger post-commit notifications.
   *
   * @param ag the asynchronous group for registering background tasks
   * @throws Err if post-commit processing fails
   */
  default void postCommit(AsyncGroup ag) throws Err {}

  /**
   * Checks whether this connection has been committed.
   *
   * @return {@code true} if this connection has been committed; {@code false} otherwise
   */
  boolean isCommitted();

  /**
   * Rolls back changes made through this connection.
   *
   * <p>This method is invoked when a transaction fails before commit completes and must restore the
   * connection or underlying resource to its pre-transaction state.
   *
   * @param ag the asynchronous group for registering background tasks
   * @throws Err if rolling back connection changes fails
   */
  void rollback(AsyncGroup ag) throws Err;

  /**
   * Notifies this connection of a transaction failure with detailed failure reports.
   *
   * <p>The default implementation does nothing. Implementations can inspect failure reports to log
   * diagnostics or take recovery actions.
   *
   * @param ag the asynchronous group for registering background tasks
   * @param reports the list of transaction failure reports
   */
  default void onTxnFailure(AsyncGroup ag, List<TxnFailureReport> reports) {}

  /** Closes and disposes of this data connection, releasing any held resources. */
  void close();

  /**
   * Terminates this connection abnormally when an unrecoverable {@link RuntimeException} or {@link
   * Error} occurs.
   *
   * <p>The default implementation delegates to {@link #close()}.
   */
  default void abend() {
    close();
  }
}
