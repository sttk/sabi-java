// Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
// This program is free software under MIT License.
// See the file LICENSE in this distribution for more details.

package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.ArrayList;

/**
 * Manages local {@link DataSrc} registrations and executes {@link Logic} units against a {@link
 * DataHub} in either non-transactional or transactional scopes.
 */
public final class LogicRunner implements AutoCloseable {
  final DataHub hub;

  LogicRunner(DataHub hub) {
    this.hub = hub;
  }

  /** Closes the underlying {@link DataHub} and releases all local data sources and connections. */
  @Override
  public void close() {
    this.hub.close();
  }

  /**
   * Registers a session-local {@link DataSrc} under the specified name if execution has not yet
   * begun.
   *
   * @param name the logical name of the data source
   * @param ds the {@link DataSrc} instance to register
   */
  public void uses(String name, DataSrc ds) {
    this.hub.run.uses(name, ds);
  }

  /**
   * Unregisters the data source associated with the specified name if execution has not yet begun.
   *
   * @param name the logical name of the data source to remove
   */
  public void disuses(String name) {
    this.hub.run.disuses(name);
  }

  /**
   * Executes the specified {@link Logic} in a single non-transactional scope.
   *
   * @param <D> the type of data access interface expected by the logic
   * @param logic the logic unit to execute
   * @throws Err if the runner is in an abnormal state, local data source setup fails, or {@code
   *     logic} throws an {@link Err}
   */
  @SuppressWarnings("unchecked")
  public <D> void run(Logic<D> logic) throws Err {
    if (this.hub.run.isAbnormal()) {
      throw new Err(new DataHubError.AbnormalState());
    }

    this.hub.run.begin();

    try {
      logic.run((D) this.hub);
      this.hub.run.end();
    } catch (Err err) {
      this.hub.run.end();
      throw err;
    } catch (RuntimeException e) {
      this.hub.run.abend();
      throw e;
    } catch (Throwable t) {
      this.hub.run.abend();
      throw Error.class.cast(t);
    }
  }

  /**
   * Starts a sequential non-transactional {@link LogicRun} chain.
   *
   * @return a new {@link LogicRun} instance
   */
  public LogicRun start() {
    return new LogicRun(this.hub, false);
  }

  /**
   * Executes the specified {@link Logic} within a transaction, automatically handling pre-commit,
   * commit, post-commit, rollback, and failure reporting.
   *
   * @param <D> the type of data access interface expected by the logic
   * @param logic the logic unit to execute within the transaction
   * @throws Err if the runner is in an abnormal state, local data source setup fails, {@code logic}
   *     throws an {@link Err}, or committing the transaction fails
   */
  @SuppressWarnings("unchecked")
  public <D> void txn(Logic<D> logic) throws Err {
    if (this.hub.run.isAbnormal()) {
      throw new Err(new DataHubError.AbnormalState());
    }

    this.hub.run.begin();

    ArrayList<TxnFailureReport.Builder> builders = null;
    try {
      logic.run((D) this.hub);
      builders = this.hub.run.newTxnFailureReportBuilders(null);
      this.hub.run.commit(builders);
      this.hub.run.end();
    } catch (Err err) {
      if (builders == null) {
        builders = this.hub.run.newTxnFailureReportBuilders(err);
      }
      try {
        this.hub.run.rollback(builders);
        this.hub.run.end();
      } catch (Throwable t) {
        this.hub.run.abend();
        throw t;
      }
      throw err;
    } catch (RuntimeException e) {
      if (builders == null) {
        var err = new Err(new LogicTxnError.RuntimeExceptionOccurred(), e);
        builders = this.hub.run.newTxnFailureReportBuilders(err);
      }
      try {
        this.hub.run.rollback(builders);
      } catch (Throwable t) {
        // do nothing
      } finally {
        this.hub.run.abend();
      }
      throw e;
    } catch (Throwable t) {
      if (builders == null) {
        var err = new Err(new LogicTxnError.ErrorOccurred(), t);
        builders = this.hub.run.newTxnFailureReportBuilders(err);
      }
      try {
        this.hub.run.rollback(builders);
      } catch (Throwable tt) {
        // do nothing
      } finally {
        this.hub.run.abend();
      }
      throw Error.class.cast(t);
    }
  }

  /**
   * Begins a sequential transactional {@link LogicTxn} chain.
   *
   * @return a new {@link LogicTxn} instance
   */
  public LogicTxn beginTxn() {
    return new LogicTxn(this.hub);
  }
}
