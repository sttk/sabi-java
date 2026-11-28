/*
 * AsyncGroup.java
 * Copyright (C) 2023-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages asynchronous background tasks executed during data source and data connection lifecycle
 * events.
 *
 * <p>An instance of {@code AsyncGroup} is passed to methods of {@link DataSrc} (such as {@link
 * DataSrc#setup(AsyncGroup)}) and {@link DataConn} (such as {@link DataConn#preCommit(AsyncGroup)},
 * {@link DataConn#commit(AsyncGroup)}, {@link DataConn#postCommit(AsyncGroup)}, {@link
 * DataConn#rollback(AsyncGroup)}, and {@link DataConn#onTxnFailure(AsyncGroup, List)}).
 * Implementations of data sources and data connections can register asynchronous operations using
 * {@link #add(Task)}.
 *
 * <p>Registered {@link Task} instances are executed on virtual threads and joined when the
 * lifecycle phase completes. Any {@link Err}, {@link RuntimeException}, or {@link Throwable} thrown
 * by a task is captured and reported via {@link AsyncGroupError} and {@link ErrEntry}.
 *
 * <p>An instance of this class is considered consumed after {@link #join()} is called and cannot be
 * reused.
 */
public final class AsyncGroup {

  private List<ErrEntry> eeList = new ArrayList<>();
  private VthEntry vthHead;
  private VthEntry vthLast;
  int index;
  String name;

  AsyncGroup() {}

  /**
   * Adds a task to be executed asynchronously on a virtual thread within this group.
   *
   * @param task the {@link Task} to execute asynchronously
   */
  public void add(Task task) {
    if (eeList == null) {
      throw new IllegalStateException("This AsyncGroup has already been joined.");
    }

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
    if (eeList == null) {
      throw new IllegalStateException("This AsyncGroup has already been joined.");
    }

    boolean interrupted = false;
    for (var ve = this.vthHead; ve != null; ve = ve.next) {
      InterruptedException firstInterruption = null;
      while (true) {
        try {
          ve.thread.join();
          break;
        } catch (InterruptedException e) {
          if (firstInterruption == null) {
            firstInterruption = e;
          } else {
            firstInterruption.addSuppressed(e);
          }
        }
      }
      if (firstInterruption != null) {
        interrupted = true;
        var err = new Err(new AsyncGroupError.TaskInterrupted(), firstInterruption);
        addErr(ve.index, ve.name, err);
      }

      try {
        if (ve.thrown != null) {
          throw ve.thrown;
        }
      } catch (RuntimeException e) {
        addErr(ve.index, ve.name, new Err(new AsyncGroupError.RuntimeExceptionOccurred(), e));
      } catch (Throwable t) {
        addErr(ve.index, ve.name, new Err(new AsyncGroupError.ErrorOccurred(), t));
      }
    }

    var errors = this.eeList;
    this.eeList = null;
    this.vthHead = null;
    this.vthLast = null;

    if (interrupted) {
      Thread.currentThread().interrupt();
    }
    return errors;
  }

  /**
   * Functional interface representing an asynchronous unit of work registered with an {@link
   * AsyncGroup}.
   */
  @FunctionalInterface
  public interface Task {

    /**
     * Executes the asynchronous task.
     *
     * @throws Err if an error occurs during task execution
     */
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
