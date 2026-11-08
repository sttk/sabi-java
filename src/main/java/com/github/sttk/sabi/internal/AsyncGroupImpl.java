/*
 * AsyncGroupImpl.java
 * Copyright (C) 2023-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import static com.github.sttk.sabi.AsyncGroup.ErrorOccurred;
import static com.github.sttk.sabi.AsyncGroup.RuntimeExceptionOccurred;
import static com.github.sttk.sabi.AsyncGroup.Task;
import static com.github.sttk.sabi.AsyncGroup.TaskInterrupted;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.ErrEntry;
import java.util.ArrayList;
import java.util.List;

public final class AsyncGroupImpl extends AsyncGroup {
  private List<ErrEntry> eeList = new ArrayList<>();
  private VthEntry vthHead;
  private VthEntry vthLast;

  int index;
  String name;

  AsyncGroupImpl() {}

  @Override
  public void add(Task task) {
    final var ix = this.index;
    final var nm = this.name;

    var ve = new VthEntry(ix, nm);
    ve.thread =
        Thread.ofVirtual()
            .uncaughtExceptionHandler(
                (t, e) -> {
                  ve.thrown = e;
                })
            .start(
                () -> {
                  try {
                    task.execute();
                  } catch (Err err) {
                    addErr(ix, nm, err);
                  }
                });

    if (this.vthLast == null) {
      this.vthHead = ve;
      this.vthLast = ve;
    } else {
      this.vthLast.next = ve;
      this.vthLast = ve;
    }
  }

  synchronized void addErr(int index, String name, Err err) {
    var ee = new ErrEntry(index, name, err);
    this.eeList.add(ee);
  }

  List<ErrEntry> join() {
    for (var ve = this.vthHead; ve != null; ve = ve.next) {
      try {
        ve.thread.join();
        if (ve.thrown != null) {
          throw ve.thrown;
        }
      } catch (InterruptedException e) {
        addErr(ve.index, ve.name, new Err(new TaskInterrupted(), e));
      } catch (RuntimeException e) {
        addErr(ve.index, ve.name, new Err(new RuntimeExceptionOccurred(), e));
      } catch (Throwable t) {
        addErr(ve.index, ve.name, new Err(new ErrorOccurred(), t));
      }
    }
    return this.eeList;
  }

  private class VthEntry {
    final int index;
    final String name;
    Thread thread;
    Throwable thrown;
    VthEntry next;

    VthEntry(int index, String name) {
      this.index = index;
      this.name = name;
    }
  }
}
