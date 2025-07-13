/*
 * Runner class.
 * Copyright (C) 2022-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Exc;

/** {@code Runner} is the interface that runs any procedure. */
@FunctionalInterface
public interface Runner {

  /**
   * Runs the procedure that this instance represents. This method takes no argument and returns
   * nothing. And this method throws an {@link Exc} exception if this method failed.
   *
   * @throws Exc If this method failed.
   */
  void run() throws Exc;
}
