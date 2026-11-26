// Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
// This program is free software under MIT License.
// See the file LICENSE in this distribution for more details.

package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.ArrayList;

/**
 * Executes a sequence of {@link Logic} units in a non-transactional scope.
 *
 * <p>A {@code LogicRun} is started via {@link LogicRunner#start()} or {@link DataAcc#start()} and
 * terminated by calling {@link #end()}. Logic units can be chained with {@link #run(Logic)}, {@link
 * #runForce(Logic)}, and {@link #runOrBlock(Logic)} to control error propagation across steps.
 */
public final class LogicRun {

  private final DataHub hub;
  private final boolean nested;
  private final ArrayList<ErrEntry> errors;
  private LogicErrAt errAt;
  private int index;

  private enum LogicErrAt {
    Begin,
    Run,
    Block,
  }

  LogicRun(DataHub hub, boolean nested) {
    this.hub = hub;
    this.index = 0;
    this.nested = nested;
    this.errors = new ArrayList<>();

    if (!nested) {
      try {
        this.hub.run.begin();
        this.errAt = LogicErrAt.Run;
      } catch (Err err) {
        this.errors.add(new ErrEntry(0, "", err));
        this.errAt = LogicErrAt.Begin;
      }
    } else {
      this.errAt = LogicErrAt.Run;
    }
  }

  /**
   * Executes the specified {@link Logic} if no prior {@link Err} has occurred and execution has not
   * been blocked or terminated abnormally.
   *
   * <p>If {@code logic} throws an {@link Err}, it is recorded and reported when {@link #end()} is
   * called. If an unhandled {@link RuntimeException} or {@link Throwable} is thrown, the underlying
   * {@link DataHub} is terminated abnormally and the exception or error is rethrown.
   *
   * @param <D> the type of data access interface expected by the logic
   * @param logic the logic unit to execute
   * @return this {@code LogicRun} instance for method chaining
   */
  @SuppressWarnings("unchecked")
  public <D> LogicRun run(Logic<D> logic) {
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
            var name = String.format("LogicRun#run(logic-%d)", index);
            this.errors.add(new ErrEntry(index, name, err));
          } catch (RuntimeException e) {
            this.hub.run.abend();
            throw e;
          } catch (Throwable t) {
            this.hub.run.abend();
            throw Error.class.cast(t);
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
   * @return this {@code LogicRun} instance for method chaining
   */
  @SuppressWarnings("unchecked")
  public <D> LogicRun runForce(Logic<D> logic) {
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
          var name = String.format("LogicRun#runForce(logic-%d)", index);
          this.errors.add(new ErrEntry(index, name, err));
        } catch (RuntimeException e) {
          this.hub.run.abend();
          throw e;
        } catch (Throwable t) {
          this.hub.run.abend();
          throw Error.class.cast(t);
        }
        return this;
      default:
        return this;
    }
  }

  /**
   * Executes the specified {@link Logic} if no prior {@link Err} has occurred, and blocks all
   * subsequent {@link #run(Logic)}, {@link #runForce(Logic)}, and {@link #runOrBlock(Logic)} calls
   * in this chain if it throws an {@link Err}.
   *
   * @param <D> the type of data access interface expected by the logic
   * @param logic the logic unit to execute
   * @return this {@code LogicRun} instance for method chaining
   */
  @SuppressWarnings("unchecked")
  public <D> LogicRun runOrBlock(Logic<D> logic) {
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
            var name = String.format("LogicRun#runOrBlock(logic-%d)", index);
            this.errors.add(new ErrEntry(index, name, err));
            this.errAt = LogicErrAt.Block;
          } catch (RuntimeException e) {
            this.hub.run.abend();
            throw e;
          } catch (Throwable t) {
            this.hub.run.abend();
            throw Error.class.cast(t);
          }
        }
        return this;
      default:
        return this;
    }
  }

  /**
   * Completes this {@code LogicRun} sequence, closing connections created during a top-level run
   * and throwing an {@link Err} if setup failed, any chained logic failed, or the {@link DataHub}
   * is in an abnormal state.
   *
   * @throws Err if local data source setup failed, one or more logic executions threw an {@link
   *     Err}, or the {@link DataHub} is in an abnormal state
   */
  public void end() throws Err {
    if (this.hub.run.isAbnormal()) {
      throw new Err(new DataHubError.AbnormalState());
    }

    if (!this.nested) {
      try {
        this.hub.run.end();
      } catch (RuntimeException e) {
        this.hub.run.abend();
        throw e;
      } catch (Throwable t) {
        this.hub.run.abend();
        throw Error.class.cast(t);
      }
    }

    switch (this.errAt) {
      case Begin:
        throw this.errors.get(0).err();
      case Run:
        if (this.errors.isEmpty()) {
          return;
        } else {
          throw new Err(new DataHubError.FailToRunLogic(this.errors));
        }
      default: // Block
        throw new Err(new DataHubError.FailToRunLogic(this.errors));
    }
  }
}
