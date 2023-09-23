/*
 * AsyncGroupSync class.
 * Copyright (C) 2023 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.async;

import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.Runner;
import com.github.sttk.sabi.errs.Err;

/**
 * AsyncGroupSync is the class to run added {@link Runner}(s) on {@link
 * AsyncGroup} API but synchronously.
 */
public class AsyncGroupSync implements AsyncGroup {

  /** An error result when a {@link Runner} object runs. */
  private Err err = null;

  /**
   * The default constructor.
   */
  public AsyncGroupSync() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void add(final Runner runner) {
    try { 
      runner.run();
    } catch (Err e) {
      this.err = e;
    } catch (Exception e) {
      this.err = new Err(new RunnerFailed(), e);
    }
  }

  /**
   * Gets a {@link Err} object that this instance holds.
   *
   * @return  A {@link Err} object.
   */
  public Err getErr() {
    return this.err;
  }
}
