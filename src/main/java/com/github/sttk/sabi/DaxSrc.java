/*
 * DaxSrc class.
 * Copyright (C) 2022-2023 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.sabi.errs.Err;

/**
 * {@code DaxSrc} is the interface that represents a data source like database,
 * etc.,and creates a {@link DaxConn} which is a connection to the data source.
 * This interface declares three methods: {@link #setup}, {@link #close}, and
 * {@link #createDaxConn}.
 *
 * {@link #setup} is the method to connect to a data store and to prepare to
 * create {@link DaxConn} objects which represents a connection to access data
 * in the data store.
 * If the set up procedure is asynchronous, the {@link #setup} method is
 * implemented so as to use {@link AsyncGroup}.
 * {@link #close} is the method to disconnect to a data store.
 * {@link #createDaxConn} is the method to create a {@link DaxConn} object.
 */
public interface DaxSrc {
  /**
   * Sets up this data source.
   *
   * @param ag  An {@link AsyncGroup} object which is used if a setting up
   *   process is asynchronous.
   * @throws Err  If setting up pprocess is synchronous and fails.
   */
  void setup(AsyncGroup ag) throws Err;

  /**
   * Closes this data source.
   */
  void close();

  /**
   * Creates a {@link DaxConn} object which represents a connection to this
   * data source.
   *
   * @return  A {@link DaxConn} object.
   * @throws Err  If creating a {@link DaxConn} fails.
   */
  DaxConn createDaxConn() throws Err;
}
