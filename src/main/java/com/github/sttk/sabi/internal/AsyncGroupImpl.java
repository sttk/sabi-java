/*
 * AsyncGroupImpl.java
 * Copyright (C) 2023-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import static com.github.sttk.sabi.AsyncGroup.RunnerInterrupted;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.ErrEntry;
import com.github.sttk.sabi.Runner;
import java.util.ArrayList;
import java.util.List;

public final class AsyncGroupImpl implements AsyncGroup {
  private List<ErrEntry> eeList = new ArrayList<>();
  private VthEntry vthHead;
  private VthEntry vthLast;
  int _index;
  String _name;

  AsyncGroupImpl() {}

  @Override
  public void add(Runner runner) {
    var index = this._index;
    var name = this._name;
    var vth =
        Thread.ofVirtual()
            .start(
                () -> {
                  try {
                    runner.run();
                  } catch (Err err) {
                    addErr(index, name, err);
                  } catch (RuntimeException e) {
                    addErr(index, name, e);
                  }
                });

    var ve = new VthEntry(index, name, vth);
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

  synchronized void addErr(int index, String name, RuntimeException e) {
    var err = new Err(new AsyncGroup.RuntimeExceptionOccured(), e);
    var ee = new ErrEntry(index, name, err);
    this.eeList.add(ee);
  }

  List<ErrEntry> join() {
    for (var ve = this.vthHead; ve != null; ve = ve.next) {
      try {
        ve.thread.join();
      } catch (InterruptedException e) {
        addErr(ve.index, ve.name, new Err(new RunnerInterrupted(), e));
      }
    }
    return this.eeList;
  }
}

class VthEntry {
  final int index;
  final String name;
  final Thread thread;
  VthEntry next;

  VthEntry(int index, String name, Thread thread) {
    this.index = index;
    this.name = name;
    this.thread = thread;
  }
}
