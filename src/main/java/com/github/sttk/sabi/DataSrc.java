/*
 * DataSrc.java
 * Copyright (C) 2022-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

/**
 * Represents a data source corresponding to an external data resource, serving as a factory for
 * data connections or as a connection pool.
 *
 * <p>Implementations encapsulate connection management, pool configuration, and resource allocation
 * for specific data stores or external services (such as relational databases, HTTP clients, or
 * caching services). Data sources can be registered globally via {@link Sabi#uses(String, DataSrc)}
 * or bound locally to a {@link DataHub}.
 */
public interface DataSrc {

  /**
   * Sets up this data source.
   *
   * <p>This method is invoked during application startup or hub setup to prepare connections or
   * connection pools. Asynchronous setup background tasks can be registered using the provided
   * {@link AsyncGroup}.
   *
   * @param ag the asynchronous group for registering background tasks
   * @throws Err if setting up the data source fails
   */
  void setup(AsyncGroup ag) throws Err;

  /** Closes this data source and disposes of any allocated resources or connection pools. */
  void close();

  /**
   * Creates a new {@link DataConn} instance associated with this data source.
   *
   * @return a new, initialized {@link DataConn} connection instance
   * @throws Err if creating or establishing the data connection fails
   */
  DataConn createDataConn() throws Err;
}
