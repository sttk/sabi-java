/*
 * Runner class.
 * Copyright (C) 2022-2023 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.sabi.errs.Err;

/**
 * Runner is the interface that runs any procedure..
 */
@FunctionalInterface
public interface Runner {

  /**
   * Runs the procedure that this instance represents.
   * This method takes no argument and returns nothing.
   * And this method throws an {@link Err} exception if this method failed.
   *
   * @throws Err  If this method failed.
   */
  void run() throws Err;
}
