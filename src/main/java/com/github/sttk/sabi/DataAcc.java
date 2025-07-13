/*
 * DataAcc class.
 * Copyright (C) 2023-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Exc;

/**
 * An interface designed for implementing data access operations through default methods in its
 * sub-interfaces.
 *
 * <p>Sub-interfaces of {@code DataAcc} are expected to define and implement data access methods as
 * default methods. Within these default methods, the connection to the underlying data store should
 * be obtained using the {@link #getDataConn(String, Class)} method provided by this interface. This
 * design promotes a clear separation of concerns, allowing data access logic to be encapsulated
 * within the interface itself.
 */
public interface DataAcc {
  /**
   * Retrieves a connection to a data store. This method is intended to be used by default methods
   * in sub-interfaces to obtain the necessary connection for performing data access operations.
   *
   * @param <C> The type of the data connection, which must extend {@link DataConn}.
   * @param name The name identifying the specific data connection to retrieve.
   * @param cls The {@link Class} object representing the type of the desired data connection.
   * @return A data connection object of the specified type.
   * @throws Exc if an error occurs while obtaining the data connection.
   */
  <C extends DataConn> C getDataConn(String name, Class<C> cls) throws Exc;
}
