/*
 * DataAcc.java
 * Copyright (C) 2023-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

/**
 * Provides access to named {@link DataConn} instances and nested logic execution within a data
 * access scope.
 *
 * <p>Application logic uses this interface (or a custom interface extending it) to retrieve managed
 * data connections and to invoke sub-logic units within the same execution context.
 */
public interface DataAcc {

  /**
   * Retrieves a data connection associated with the specified data source name and casts it to the
   * requested connection class type.
   *
   * <p>If a connection for the given {@code name} already exists in the current execution scope,
   * that connection instance is returned. Otherwise, a new connection is created from the
   * corresponding registered {@link DataSrc}.
   *
   * @param <C> the expected type of {@link DataConn}
   * @param name the registered logical name of the data source
   * @param cls the {@link Class} representing the target connection type {@code C}
   * @return the data connection instance associated with the specified name
   * @throws Err if no data source is registered for {@code name}, if creating the connection fails
   *     or returns {@code null}, or if the connection cannot be cast to {@code cls}
   */
  <C extends DataConn> C getDataConn(String name, Class<C> cls) throws Err;

  /**
   * Executes the specified {@link Logic} within the current data access context.
   *
   * @param <D> the type of data access interface expected by the logic
   * @param logic the logic unit to execute
   * @throws Err if an error occurs while executing {@code logic}
   */
  <D> void run(Logic<D> logic) throws Err;

  /**
   * Starts a sequential {@link LogicRun} chain within the current data access context.
   *
   * @return a new {@link LogicRun} instance for chaining logic executions
   */
  LogicRun start();
}
