/*
 * DataAcc.java
 * Copyright (C) 2023-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

/**
 * Provides access to named {@link DataConn} data connection instances.
 *
 * <p>This interface defines the contract for retrieving managed data connections registered under
 * specific names within a data access scope (such as a {@link DataHub}). Application business logic
 * uses this interface to obtain connection objects required to perform database or external service
 * operations.
 */
public interface DataAcc {

  /**
   * Retrieves a data connection associated with the specified data source name and casts it to the
   * requested connection class type.
   *
   * <p>If a connection for the given {@code name} has already been created within the current
   * transaction or execution scope, that connection instance is returned. Otherwise, a new
   * connection is created via the corresponding registered data source.
   *
   * @param <C> the expected type of {@link DataConn}
   * @param name the registered logical name of the data source
   * @param cls the {@link Class} representing the target connection type {@code C}
   * @return the data connection instance associated with the specified name
   * @throws Err if no data source with the specified name is found, if creating the connection
   *     fails or yields null, or if the connection cannot be cast to the target type
   */
  <C extends DataConn> C getDataConn(String name, Class<C> cls) throws Err;
}
