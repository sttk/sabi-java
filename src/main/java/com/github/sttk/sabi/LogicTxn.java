// Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
// This program is free software under MIT License.
// See the file LICENSE in this distribution for more details.

package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.ArrayList;

/**
 * Executes a sequence of {@link Logic} units within a single transaction scope.
 *
 * <p>A {@code LogicTxn} is started via {@link LogicRunner#beginTxn()} and completed by calling
 * {@link #endTxn()}. If all executed logic units succeed without {@link Err}, connections are
 * committed; otherwise, uncommitted connections are rolled back and failure reports are dispatched.
 */
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

  /**
   * Executes the specified {@link Logic} if no prior {@link Err} has occurred and execution has not
   * been blocked or terminated abnormally.
   *
   * <p>If {@code logic} throws an {@link Err}, it is recorded and the transaction will be rolled
   * back when {@link #endTxn()} is called. If an unhandled {@link RuntimeException} or {@link
   * Throwable} is thrown, uncommitted connections are rolled back, the {@link DataHub} is
   * terminated abnormally, and the exception or error is rethrown.
   *
   * @param <D> the type of data access interface expected by the logic
   * @param logic the logic unit to execute
   * @return this {@code LogicTxn} instance for method chaining
   */
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

  /**
   * Executes the specified {@link Logic} even if a prior {@link #run(Logic)} or {@link
   * #runForce(Logic)} call recorded an {@link Err}, provided execution has not been blocked by
   * {@link #runOrBlock(Logic)}, setup did not fail, and the {@link DataHub} is not in an abnormal
   * state.
   *
   * @param <D> the type of data access interface expected by the logic
   * @param logic the logic unit to execute
   * @return this {@code LogicTxn} instance for method chaining
   */
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

  /**
   * Executes the specified {@link Logic} if no prior {@link Err} has occurred, and blocks all
   * subsequent {@link #run(Logic)}, {@link #runForce(Logic)}, and {@link #runOrBlock(Logic)} calls
   * in this transaction if it throws an {@link Err}.
   *
   * @param <D> the type of data access interface expected by the logic
   * @param logic the logic unit to execute
   * @return this {@code LogicTxn} instance for method chaining
   */
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

  /**
   * Completes this transaction by committing all connections if no errors occurred, or rolling back
   * uncommitted connections and closing resources otherwise.
   *
   * @throws Err if the {@link DataHub} is in an abnormal state, local data source setup failed, one
   *     or more logic executions failed, or committing the transaction failed
   */
  public void endTxn() throws Err {
    if (this.hub.run.isAbnormal()) {
      throw new Err(new DataHubError.AbnormalState());
    }

    try {
      switch (this.errAt) {
        case Begin:
          this.hub.run.end();
          throw this.errors.get(0).err();
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
