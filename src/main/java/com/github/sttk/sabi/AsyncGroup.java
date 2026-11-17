/*
 * AsyncGroup.java
 * Copyright (C) 2023-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.ArrayList;
import java.util.List;

public final class AsyncGroup {

  private List<ErrEntry> eeList = new ArrayList<>();
  private VthEntry vthHead;
  private VthEntry vthLast;
  int index;
  String name;

  AsyncGroup() {}

  public void add(Task task) {
    final int index = this.index;
    final String name = this.name;

    var ve = new VthEntry(index, name);
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
                    addErr(index, name, err);
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
        addErr(ve.index, ve.name, new Err(new AsyncGroupError.TaskInterrupted(), e));
      } catch (RuntimeException e) {
        addErr(ve.index, ve.name, new Err(new AsyncGroupError.RuntimeExceptionOccurred(), e));
      } catch (Throwable t) {
        addErr(ve.index, ve.name, new Err(new AsyncGroupError.ErrorOccurred(), t));
      }
    }
    return this.eeList;
  }

  @FunctionalInterface
  public interface Task {
    void execute() throws Err;
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
