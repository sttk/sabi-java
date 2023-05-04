/*
 * DaxSrc claass.
 * Copyright (C) 2022-2023 Takayuki Sato. All Rights Reserved.
 */
package sabi;

/**
 * DaxSrc is an interface which represents a data source like database, etc.,
 * and creates a {@link DaxConn} to the data source.
 * The class inheriting this requires a method: {@link #createDaxConn} to do
 * so.
 */
public interface DaxSrc {

  /**
   * Creates a {@link DaxConn} object which is a connection to a data source
   * which the instance of this class indicates.
   *
   * @return  a {@link DaxConn} object.
   * @throws Err  If this instance failed to create a {@link DaxConn} object.
   */
  DaxConn createDaxConn() throws Err;

  /**
   * Makes available this data source.
   *
   * For example, connecting to the data store, setting up connection pooling,
   * and so on.
   *
   * @throws Err  If this instance failed to set up.
   */
  void setUp() throws Err;

  /**
   * Frees the resources of this data source.
   */
  void end();
}
