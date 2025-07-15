/*
 * AsyncGroupImpl.java
 * Copyright (C) 2023-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.Runner;
import java.util.Map;

public class AsyncGroupImpl implements AsyncGroup {
  private ExcEntry excHead;
  private ExcEntry excLast;
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
                  } catch (Exc | RuntimeException e) {
                    addExc(name, e);
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

  synchronized void addExc(String name, Exception e) {
    var exc = (e instanceof Exc) ? Exc.class.cast(e) : new Exc(new RunnerFailed(), e);
    var ent = new ExcEntry(name, exc);

    if (this.excLast == null) {
      this.excHead = ent;
      this.excLast = ent;
    } else {
      this.excLast.next = ent;
      this.excLast = ent;
    }
  }

  void joinAndPutExcsInto(Map<String, Exc> excMap) {
    for (var ent = this.vthHead; ent != null; ent = ent.next) {
      try {
        ent.thread.join();
      } catch (InterruptedException e) {
        addExc(ent.name, new Exc(new RunnerInterrupted(), e));
      }
    }
    for (var ent = this.excHead; ent != null; ent = ent.next) {
      excMap.put(ent.name, ent.exc);
    }
    clear();
  }

  void joinAndIgnoreExcs() {
    for (var ent = this.vthHead; ent != null; ent = ent.next) {
      try {
        ent.thread.join();
      } catch (InterruptedException e) {
      }
    }
    clear();
  }

  void clear() {
    this.excHead = null;
    this.excLast = null;
    this.vthHead = null;
    this.vthLast = null;
  }
}

class ExcEntry {
  final String name;
  final Exc exc;
  ExcEntry next;

  ExcEntry(String name, Exc exc) {
    this.name = name;
    this.exc = exc;
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
