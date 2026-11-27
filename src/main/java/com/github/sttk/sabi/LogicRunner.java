/*
 * LogicRunner.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
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
    this.hub.dataRun.uses(name, ds);
  }

  /**
   * Unregisters the data source associated with the specified name if execution has not yet begun.
   *
   * @param name the logical name of the data source to remove
   */
  public void disuses(String name) {
    this.hub.dataRun.disuses(name);
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
    if (this.hub.dataRun.isAbnormal()) {
      throw new Err(new DataHubError.AbnormalState());
    }

    this.hub.dataRun.begin();

    Err logicErr = null;
    try {
      logic.run((D) this.hub);
    } catch (Err err) {
      logicErr = err;
    } catch (RuntimeException e) {
      this.hub.dataRun.abend();
      throw e;
    } catch (Throwable t) {
      this.hub.dataRun.abend();
      throw Error.class.cast(t);
    }

    try {
      this.hub.dataRun.end();
    } catch (RuntimeException e) {
      this.hub.dataRun.abend();
      throw e;
    } catch (Throwable t) {
      this.hub.dataRun.abend();
      throw Error.class.cast(t);
    }

    if (logicErr != null) {
      throw logicErr;
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
    if (this.hub.dataRun.isAbnormal()) {
      throw new Err(new DataHubError.AbnormalState());
    }

    this.hub.dataRun.begin();

    ArrayList<TxnFailureReport.Builder> builders = null;
    try {
      logic.run((D) this.hub);
      builders = this.hub.dataRun.newTxnFailureReportBuilders(null);
      this.hub.dataRun.commit(builders);
      this.hub.dataRun.end();
    } catch (Err err) {
      if (builders == null) {
        builders = this.hub.dataRun.newTxnFailureReportBuilders(err);
      }
      try {
        this.hub.dataRun.rollback(builders);
        this.hub.dataRun.end();
      } catch (Throwable t) {
        this.hub.dataRun.abend();
        throw t;
      }
      throw err;
    } catch (RuntimeException e) {
      if (builders == null) {
        var err = new Err(new LogicTxnError.RuntimeExceptionOccurred(), e);
        builders = this.hub.dataRun.newTxnFailureReportBuilders(err);
      }
      try {
        this.hub.dataRun.rollback(builders);
      } catch (Throwable t) {
        e.addSuppressed(t);
      } finally {
        this.hub.dataRun.abend();
      }
      throw e;
    } catch (Throwable t) {
      if (builders == null) {
        var err = new Err(new LogicTxnError.ErrorOccurred(), t);
        builders = this.hub.dataRun.newTxnFailureReportBuilders(err);
      }
      try {
        this.hub.dataRun.rollback(builders);
      } catch (Throwable tt) {
        t.addSuppressed(tt);
      } finally {
        this.hub.dataRun.abend();
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
