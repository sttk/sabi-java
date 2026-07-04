/*
 * AsyncGroup.java
 * Copyright (C) 2023-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.sabi.internal.AsyncGroupImpl;

/**
 * Manages asynchronous background tasks executed during data source and data connection lifecycle
 * events.
 *
 * <p>An instance of {@code AsyncGroup} is passed to methods of {@link DataSrc} (such as {@link
 * DataSrc#setup(AsyncGroup)}) and {@link DataConn} (such as {@link DataConn#commit(AsyncGroup)} and
 * {@link DataConn#rollback(AsyncGroup)}). Implementations of data sources and data connections can
 * register background asynchronous operations (such as parallel cleanup or pre-commit validations)
 * using the {@link #add(Runner)} method.
 *
 * <p>All registered {@link Runner} tasks are managed by this group and executed asynchronously. If
 * any registered task fails, is interrupted, or throws an unhandled runtime exception,
 * corresponding error records defined in this interface are produced to report the failure.
 */
public sealed interface AsyncGroup permits AsyncGroupImpl {

  /** Indicates that a registered {@link Runner} task failed during its execution. */
  record RunnerFailed() {}

  /** Indicates that a registered {@link Runner} task execution was interrupted. */
  record RunnerInterrupted() {}

  /**
   * Indicates that an unhandled runtime exception was thrown during the execution of a registered
   * {@link Runner} task.
   */
  record RuntimeExceptionOccured() {}

  /**
   * Adds a background task to be executed asynchronously by this group.
   *
   * <p>The task is encapsulated in a {@link Runner} functional interface and scheduled for parallel
   * asynchronous execution.
   *
   * @param runner the {@link Runner} task to be added and executed asynchronously
   */
  void add(Runner runner);
}
