/*
 * Logic.java
 * Copyright (C) 2022-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

/**
 * Functional interface representing a unit of business logic executed with a data access interface.
 *
 * <p>Instances of this interface are passed to {@link DataAcc#run(Logic)}, {@link
 * LogicRunner#run(Logic)}, {@link LogicRunner#txn(Logic)}, {@link LogicRun}, or {@link LogicTxn}.
 *
 * @param <D> the type of data access interface passed to the logic execution method
 */
@FunctionalInterface
public interface Logic<D> {

  /**
   * Executes the business logic using the provided data access instance.
   *
   * @param data the data access instance (typically a {@link DataHub} implementing a custom data
   *     access interface)
   * @throws Err if an error occurs during logic execution
   */
  void run(D data) throws Err;
}
