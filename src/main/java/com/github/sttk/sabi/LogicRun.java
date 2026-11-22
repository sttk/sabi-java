// Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
// This program is free software under MIT License.
// See the file LICENSE in this distribution for more details.

package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.ArrayList;

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
        throw this.errors.get(0).err;
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
