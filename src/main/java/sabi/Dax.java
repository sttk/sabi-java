/*
 * Dax class.
 * Copyright (C) 2022 Takayuki Sato. All Rights Reserved.
 */
package sabi;

/**
 * Dax is an interface for a set of data accesses, and requires a method:
 * {@link #getDaxConn} which gets a connection to an external data access.
 */
public interface Dax {

  /**
   * Gets a {@link DaxConn} which is a connection to a data source by specified
   * name.
   *
   * @param name  The name of {@link DaxConn} or {@link DaxSrc}.
   * @return  A {@link DaxConn} object.
   * @throws Err  If failing to get {@link DaxConn}.
   */
  DaxConn getDaxConn(final String name) throws Err;
}
