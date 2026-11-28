/*
 * DataSrc.java
 * Copyright (C) 2022-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

/**
 * Represents a data source corresponding to an external resource, serving as a factory for {@link
 * DataConn} instances or managing a connection pool.
 *
 * <p>Implementations encapsulate resource initialization, connection creation, and cleanup for
 * external systems such as relational databases, caches, or messaging services. Data sources can be
 * registered globally via {@link Globals#uses(String, DataSrc)} or locally via {@link
 * LogicRunner#uses(String, DataSrc)}.
 */
public interface DataSrc {

  /**
   * Sets up this data source before connections are created from it.
   *
   * <p>Asynchronous setup tasks may be registered with the provided {@link AsyncGroup}.
   *
   * @param ag the asynchronous group for registering background setup tasks
   * @throws Err if setting up this data source fails
   */
  void setup(AsyncGroup ag) throws Err;

  /** Closes this data source and releases any resources or connection pools it manages. */
  void close();

  /**
   * Creates a new {@link DataConn} instance backed by this data source.
   *
   * @return a newly created {@link DataConn}
   * @throws Err if creating the data connection fails
   */
  DataConn createDataConn() throws Err;
}
