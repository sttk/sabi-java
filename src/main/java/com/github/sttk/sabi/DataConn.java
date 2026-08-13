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
 * through pre-commit, commit, and post-commit phases, and support rollback and failure reporting
 * when errors occur.
 *
 * @since 1.0
 */
public interface DataConn {

  /**
   * Represents an error when one or more data connections fail during the pre-commit phase.
   *
   * @param errors the list of error entries detailing connection names and failure causes
   */
  public record FailToPreCommitDataConn(List<ErrEntry> errors) {}

  /**
   * Represents an error when one or more data connections fail during the commit phase.
   *
   * @param errors the list of error entries detailing connection names and failure causes
   */
  public record FailToCommitDataConn(List<ErrEntry> errors) {}

  /**
   * Represents an error when one or more data connections fail during the post-commit phase.
   *
   * @param errors the list of error entries detailing connection names and failure causes
   */
  public record FailToPostCommitDataConn(List<ErrEntry> errors) {}

  //

  /**
   * Commits changes made through this connection.
   *
   * <p>Async background tasks may be registered to the provided {@link AsyncGroup} during commit
   * processing.
   *
   * @param ag the asynchronous group for registering background tasks
   * @throws Err if committing the transaction changes fails
   */
  void commit(AsyncGroup ag) throws Err;

  /**
   * Prepares this connection for commit prior to the main commit phase.
   *
   * <p>The default implementation does nothing. Subclasses can override this method to perform
   * pre-commit checks or flush pending writes.
   *
   * @param ag the asynchronous group for registering background tasks
   * @throws Err if pre-commit preparation or validation fails
   */
  default void preCommit(AsyncGroup ag) throws Err {}

  /**
   * Performs post-commit operations after all connections in a transaction have successfully
   * committed.
   *
   * <p>The default implementation does nothing. Subclasses can override this method to perform
   * cleanup or trigger post-commit notifications.
   *
   * @param ag the asynchronous group for registering background tasks
   * @throws Err if post-commit processing fails
   */
  default void postCommit(AsyncGroup ag) throws Err {}

  /**
   * Checks whether this connection has been successfully committed.
   *
   * @return {@code true} if this connection was committed; {@code false} otherwise
   */
  boolean isCommitted();

  /**
   * Rolls back changes made through this connection.
   *
   * <p>This method is invoked when a transaction fails and must restore the connection or
   * underlying storage to its pre-transaction state.
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
}
