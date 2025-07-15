/*
 * DataSrc.java
 * Copyright (C) 2022-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Exc;

/**
 * The interface that abstracts a data source responsible for managing connections to external data
 * services, such as databases, file systems, or messaging services.
 *
 * <p>It receives configuration for connecting to an external data service and then creates and
 * supplies a {@link DataConn} instance, representing a single session connection.
 */
public interface DataSrc {
  /**
   * Sets up the data source, performing any necessary initialization or configuration to establish
   * connectivity to the external data service. This method is typically called once at the
   * application startup.
   *
   * @param ag An {@link AsyncGroup} that can be used for asynchronous setup operations, especially
   *     if initialization is time-consuming.
   * @throws Exc if an error occurs during the setup process.
   */
  void setup(AsyncGroup ag) throws Exc;

  /**
   * Closes the data source, releasing all resources and shutting down connections managed by this
   * source. This method should be called when the application is shutting down to ensure proper
   * resource cleanup.
   */
  void close();

  /**
   * Creates and returns a new {@link DataConn} instance, representing a single session connection
   * to the external data service. Each call to this method should yield a new, independent
   * connection.
   *
   * @return A new {@link DataConn} instance for a session.
   * @throws Exc if an error occurs while creating the data connection.
   */
  DataConn createDataConn() throws Exc;
}
