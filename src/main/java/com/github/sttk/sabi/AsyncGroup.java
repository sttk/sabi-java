/*
 * AsyncGroup class.
 * Copyright (C) 2023-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

/**
 * An interface for asynchronously executing multiple {@link Runner} instances and waiting for their
 * completion.
 *
 * <p>Implementations of this interface allow adding multiple {@link Runner} objects, which are then
 * executed concurrently. The group waits until all added runners have finished their execution. Any
 * errors occurring during the execution of a {@link Runner} are stored and can be retrieved by
 * their names in a map.
 */
public interface AsyncGroup {

  /**
   * Represents the reason for a new {@link com.github.sttk.errs.Exc} exception object when an
   * exception occurred during the execution of a {@link Runner} and the exception class was not the
   * {@link com.github.sttk.errs.Exc}.
   */
  record RunnerFailed() {}

  /**
   * Represents the reason for an {@link com.github.sttk.errs.Exc} exception object when the
   * creation of a thread for asynchronous execution of a {@link Runner} fails.
   */
  record RunnerInterrupted() {}

  /**
   * Adds a {@link Runner} to this group for asynchronous execution. The added runner will be
   * executed in a separate thread.
   *
   * @param runner The {@link Runner} to be added and executed asynchronously.
   */
  void add(final Runner runner);
}
