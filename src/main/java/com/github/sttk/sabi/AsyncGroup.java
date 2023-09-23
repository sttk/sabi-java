/*
 * AsyncGroup class.
 * Copyright (C) 2023 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.sabi.errs.Err;

import java.util.Map;
import java.util.HashMap;

/**
 * {@code AsyncGroup} is the inferface to execute added {@link Runner}(s)
 * asynchronously.
 * <p>
 * The method Add is to add target {@link Runner}(s).
 * This interface is used as an argument of {@link DaxSrc#setup}, {@link
 * DaxConn#commit}, and {@link DaxConn#rollback}.
 */
public interface AsyncGroup {

  /**
   * {@code RunnerFailed} is an error reason which indicates that a {@link
   * Runner} failed.
   */
  record RunnerFailed() {}

  /** 
   * {@code RunnerInterrupted} is an error reason which indicates that a {@link
   * Runner}'s thread is interrupted.
   */
  record RunnerInterrupted() {}

  /**
   * Adds a runner to be run asynchronously.
   *
   * @param runner  A {@link Runner} object.
   */
  void add(Runner runner);
}
