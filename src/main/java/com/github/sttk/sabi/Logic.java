/*
 * Logic class.
 * Copyright (C) 2022-2023 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.sabi.errs.Err;

/**
 * Logic is the functional interface that represents a logical procedure.
 * The {@link #run} method of the class inheriting this interface implements
 * only logic processing.
 * Data access processing is described only by calling methods of the argument
 * dax, and the details are implemented elsewhere.
 *
 * @param <D>  dax
 */
@FunctionalInterface
public interface Logic<D> {

  /**
   * Runs the logical procedure represented by this class.
   *
   * This method is the entry point of the whole of this logical prcedure.
   *
   * @param dax  A Dax instance providing data access methods for this logical
   *   procedure.
   * @throws Err  If an error occurs within this logic processing.
   */
  void run(D dax) throws Err;
}
