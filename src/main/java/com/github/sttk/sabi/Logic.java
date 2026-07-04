/*
 * Logic.java
 * Copyright (C) 2022-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

/**
 * Functional interface representing a unit of business logic executed within a {@link DataHub}
 * scope.
 *
 * <p>Instances of this interface are passed to {@link DataHub#run(Logic)} for non-transactional
 * execution or {@link DataHub#txn(Logic)} for transactional execution. The provided data access
 * interface {@code data} gives access to data connections and resources needed during execution.
 *
 * @param <D> the type of data access interface passed to the logic execution method
 */
@FunctionalInterface
public interface Logic<D> {

  /**
   * Executes the business logic using the provided data access interface.
   *
   * @param data the data access interface instance (typically a {@link DataHub} or custom data
   *     access interface)
   * @throws Err if an error occurs during logic execution
   */
  void run(D data) throws Err;
}
