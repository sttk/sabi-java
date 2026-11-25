// Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
// This program is free software under MIT License.
// See the file LICENSE in this distribution for more details.

package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.ArrayList;

public final class LogicRunner implements AutoCloseable {
  final DataHub hub;

  LogicRunner(DataHub hub) {
    this.hub = hub;
  }

  @Override
  public void close() {
    this.hub.close();
  }

  public void uses(String name, DataSrc ds) {
    this.hub.run.uses(name, ds);
  }

  public void disuses(String name) {
    this.hub.run.disuses(name);
  }

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

  public LogicRun start() {
    return new LogicRun(this.hub, false);
  }

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

  public LogicTxn beginTxn() {
    return new LogicTxn(this.hub);
  }
}
