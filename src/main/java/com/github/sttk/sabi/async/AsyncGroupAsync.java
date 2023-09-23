/*
 * AsyncGroupAsync class.
 * Copyright (C) 2023 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.async;

import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.Runner;
import com.github.sttk.sabi.errs.Err;

import java.util.Map;
import java.util.HashMap;

/**
 * AsyncGroupAsync is the class to run added {@link Runner}(s) on {@link
 * AsyncGroup} API asynchronously.
 *
 * @param <N>  The type of a name of a runner or its error.
 */
public class AsyncGroupAsync<N> implements AsyncGroup {

  /** A map that holds {@link Err}(s) by {@link Runner}. */
  private Map<N, Err> errMap = new HashMap<>();

  /** A map that holds mappings of a name and a thread. */
  private Map<N, Thread> vthMap = new HashMap<>();

  /** A name which will be run. */
  public N name;

  /**
   * The default constructor.
   */
  public AsyncGroupAsync() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void add(final Runner runner) {
    final var name = this.name;
    var vth = Thread.ofVirtual().start(() -> {
      try {
        runner.run();
      } catch (Err | RuntimeException e) {
        addErr(name, e);
      }
    });
    vthMap.put(name, vth);
  }

  /**
   * Adds an {@link Exception} by a {@link Runner}.
   *
   * @param name  A name of a {@link Runner}.
   * @param exc  An {@link Err} by a {@link Runner}.
   */
  public synchronized void addErr(N name, Exception exc) {
    if (exc instanceof Err) {
      errMap.put(name, Err.class.cast(exc));
    } else {
      addErr(name, new Err(new RunnerFailed(), exc));
    }
  }

  /**
   * Checks whether there are errors by {@link Runner}(s).
   *
   * @return  {@code true} if there are errors.
   */
  public boolean hasErr() {
    return !errMap.isEmpty();
  }

  /**
   * Creates a map which holds mappings of a name and an {@link Err}.
   *
   * @return  A map of names and {@link Err}(s).
   */
  public Map<N, Err> makeErrs() {
    return errMap;
  }

  /**
   * Waits for completions of {@link Runner}'s threads.
   */
  public void join() {
    for (var ent : vthMap.entrySet()) {
      try {
        ent.getValue().join();
      } catch (InterruptedException e) {
        addErr(ent.getKey(), new Err(new RunnerInterrupted(), e));
      }
    }
  }
}
