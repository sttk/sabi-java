// Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
// This program is free software under MIT License.
// See the file LICENSE in this distribution for more details.

package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.ArrayList;

public final class LogicTxn {

  private final DataHub hub;
  private final ArrayList<ErrEntry> errors;
  private LogicErrAt errAt;
  private int index;

  private enum LogicErrAt {
    Begin,
    Run,
    Block,
  }

  LogicTxn(DataHub hub) {
    this.hub = hub;
    this.index = 0;
    this.errors = new ArrayList<>();

    try {
      this.hub.run.begin();
      this.errAt = LogicErrAt.Run;
    } catch (Err err) {
      this.errors.add(new ErrEntry(0, "", err));
      this.errAt = LogicErrAt.Begin;
    }
  }

  @SuppressWarnings("unchecked")
  public <D> LogicTxn run(Logic<D> logic) {
    int index = this.index;
    this.index++;

    if (this.hub.run.isAbnormal()) {
      return this;
    }

    switch (this.errAt) {
      case Run:
        if (this.errors.isEmpty()) {
          try {
            logic.run((D) this.hub);
          } catch (Err err) {
            var name = String.format("LogicTxn#run(logic-%d)", index);
            this.errors.add(new ErrEntry(index, name, err));
          } catch (RuntimeException e) {
            endInFatal(e, index, "run");
            throw e;
          } catch (Throwable t) {
            endInFatal(Error.class.cast(t), index, "run");
            throw t;
          }
        }
        return this;
      default:
        return this;
    }
  }

  @SuppressWarnings("unchecked")
  public <D> LogicTxn runForce(Logic<D> logic) {
    int index = this.index;
    this.index++;

    if (this.hub.run.isAbnormal()) {
      return this;
    }

    switch (this.errAt) {
      case Run:
        try {
          logic.run((D) this.hub);
        } catch (Err err) {
          var name = String.format("LogicTxn#runForce(logic-%d)", index);
          this.errors.add(new ErrEntry(index, name, err));
        } catch (RuntimeException e) {
          endInFatal(e, index, "runOrBlock");
          throw e;
        } catch (Throwable t) {
          endInFatal(Error.class.cast(t), index, "runOrBlock");
          throw t;
        }
        return this;
      default:
        return this;
    }
  }

  @SuppressWarnings("unchecked")
  public <D> LogicTxn runOrBlock(Logic<D> logic) {
    int index = this.index;
    this.index++;

    if (this.hub.run.isAbnormal()) {
      return this;
    }

    switch (this.errAt) {
      case Run:
        if (this.errors.isEmpty()) {
          try {
            logic.run((D) this.hub);
          } catch (Err err) {
            var name = String.format("LogicTxn#runOrBlock(logic-%d)", index);
            this.errors.add(new ErrEntry(index, name, err));
            this.errAt = LogicErrAt.Block;
          } catch (RuntimeException e) {
            endInFatal(e, index, "runOrBlock");
            throw e;
          } catch (Throwable t) {
            endInFatal(Error.class.cast(t), index, "runOrBlock");
            throw t;
          }
        }
        return this;
      default:
        return this;
    }
  }

  public void endTxn() throws Err {
    if (this.hub.run.isAbnormal()) {
      throw new Err(new DataHubError.AbnormalState());
    }

    try {
      switch (this.errAt) {
        case Begin:
          this.hub.run.end();
          throw this.errors.get(0).err;
        case Run:
          if (this.errors.isEmpty()) {
            var builders = this.hub.run.newTxnFailureReportBuilders(null);
            try {
              this.hub.run.commit(builders);
              this.hub.run.end();
            } catch (Err err) {
              this.hub.run.rollback(builders);
              this.hub.run.end();
              throw err;
            } catch (Throwable t) {
              this.hub.run.rollback(builders);
              throw t;
            }
          } else {
            var logicErr = new Err(new DataHubError.FailToRunLogic(this.errors));
            var builders = this.hub.run.newTxnFailureReportBuilders(logicErr);
            this.hub.run.rollback(builders);
            this.hub.run.end();
            throw logicErr;
          }
          return;
        default: // Block
          var logicErr = new Err(new DataHubError.FailToRunLogic(this.errors));
          var builders = this.hub.run.newTxnFailureReportBuilders(logicErr);
          this.hub.run.rollback(builders);
          this.hub.run.end();
          throw logicErr;
      }
    } catch (Err err) {
      throw err;
    } catch (RuntimeException e) {
      this.hub.run.abend();
      throw e;
    } catch (Throwable t) {
      this.hub.run.abend();
      throw Error.class.cast(t);
    }
  }

  void endInFatal(RuntimeException e, int index, String methodName) {
    try {
      var name = String.format("LogicTxn#%s(logic-%d)", methodName, index);
      var err = new Err(new LogicTxnError.RuntimeExceptionOccurred(), e);
      this.errors.add(new ErrEntry(index, name, err));

      var logicErr = new Err(new DataHubError.FailToRunLogic(this.errors));
      var builders = this.hub.run.newTxnFailureReportBuilders(logicErr);
      this.hub.run.rollback(builders);
    } finally {
      this.hub.run.abend();
    }
  }

  void endInFatal(Error e, int index, String methodName) {
    try {
      var name = String.format("LogicTxn#%s(logic-%d)", methodName, index);
      var err = new Err(new LogicTxnError.ErrorOccurred(), e);
      this.errors.add(new ErrEntry(index, name, err));

      var logicErr = new Err(new DataHubError.FailToRunLogic(this.errors));
      var builders = this.hub.run.newTxnFailureReportBuilders(logicErr);
      this.hub.run.rollback(builders);
    } finally {
      this.hub.run.abend();
    }
  }
}
