/*
 * AsyncGroupImpl.java
 * Copyright (C) 2023-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.Runner;
import java.util.Map;

public class AsyncGroupImpl implements AsyncGroup {
  private ErrEntry errHead;
  private ErrEntry errLast;
  private VthEntry vthHead;
  private VthEntry vthLast;
  String name;

  public AsyncGroupImpl() {}

  @Override
  public void add(final Runner runner) {
    final var name = this.name;
    var vth =
        Thread.ofVirtual()
            .start(
                () -> {
                  try {
                    runner.run();
                  } catch (Err | RuntimeException e) {
                    addErr(name, e);
                  }
                });

    var ent = new VthEntry(name, vth);
    if (this.vthLast == null) {
      this.vthHead = ent;
      this.vthLast = ent;
    } else {
      this.vthLast.next = ent;
      this.vthLast = ent;
    }
  }

  synchronized void addErr(String name, Exception e) {
    var err = (e instanceof Err) ? Err.class.cast(e) : new Err(new RunnerFailed(), e);
    var ent = new ErrEntry(name, err);

    if (this.errLast == null) {
      this.errHead = ent;
      this.errLast = ent;
    } else {
      this.errLast.next = ent;
      this.errLast = ent;
    }
  }

  void joinAndPutErrsInto(Map<String, Err> errMap) {
    for (var ent = this.vthHead; ent != null; ent = ent.next) {
      try {
        ent.thread.join();
      } catch (InterruptedException e) {
        addErr(ent.name, new Err(new RunnerInterrupted(), e));
      }
    }
    for (var ent = this.errHead; ent != null; ent = ent.next) {
      errMap.put(ent.name, ent.err);
    }
    clear();
  }

  void joinAndIgnoreErrs() {
    for (var ent = this.vthHead; ent != null; ent = ent.next) {
      try {
        ent.thread.join();
      } catch (InterruptedException e) {
      }
    }
    clear();
  }

  void clear() {
    this.errHead = null;
    this.errLast = null;
    this.vthHead = null;
    this.vthLast = null;
  }
}

class ErrEntry {
  final String name;
  final Err err;
  ErrEntry next;

  ErrEntry(String name, Err err) {
    this.name = name;
    this.err = err;
  }
}

class VthEntry {
  final String name;
  final Thread thread;
  VthEntry next;

  VthEntry(String name, Thread thread) {
    this.name = name;
    this.thread = thread;
  }
}
