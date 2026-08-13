/*
 * Runner.java
 * Copyright (C) 2022-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

/**
 * Functional interface representing an asynchronous task to be executed by an {@link AsyncGroup}.
 *
 * <p>Tasks implementing this interface are added to an {@link AsyncGroup} (via {@link
 * AsyncGroup#add(Runner)}) during data source or data connection lifecycle events (such as setup,
 * commit, or rollback) to perform parallel background processing.
 *
 * @since 1.0
 */
@FunctionalInterface
public interface Runner {

  /**
   * Runs the asynchronous background task.
   *
   * @throws Err if an error occurs during task execution
   */
  void run() throws Err;
}
