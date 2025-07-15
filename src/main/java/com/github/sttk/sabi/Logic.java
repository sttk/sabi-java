/*
 * Logic.java
 * Copyright (C) 2022-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Exc;

/**
 * Represents the application's business logic, designed to separate data access concerns from the
 * core logic.
 *
 * <p>Implementations of this functional interface should focus solely on the business logic,
 * utilizing the provided {@code data} object of type {@code D} for all data access operations. The
 * {@link #run(Object)} method should not contain any direct data access code; instead, it should
 * delegate such operations to methods of the {@code D} object.
 *
 * <p>If an exceptional condition occurs during the execution of the logic, an {@link Exc} object
 * should be thrown.
 *
 * @param <D> The type of the data access object through which data operations are performed.
 */
@FunctionalInterface
public interface Logic<D> {
  /**
   * Executes the application's business logic. This method should implement the core logic, relying
   * on the {@code data} object for all data access needs.
   *
   * @param data The data access object, providing methods for interacting with data.
   * @throws Exc if an error or exceptional condition occurs during the logic execution.
   */
  void run(D data) throws Exc;
}
