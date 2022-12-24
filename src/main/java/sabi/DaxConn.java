/*
 * DaxConn class.
 * Copyright (C) 2022 Takayuki Sato. All Rights Reserved.
 */
package sabi;

/**
 * DaxConn is an interface which represents a connection to a data source.

 * The class inheriting this class requires methods: {@link #commit},
 * {@link #rollback} and {@link #close} to work in a transaction process.
 */
public interface DaxConn {

  /**
   * Makes all changes since the previous commit/rollback permanent.
   *
   * @throws Err  If this connection failed to commit changes.
   */
  void commit() throws Err;

  /**
   * Undoes all changes since the previous commit rollback.
   */
  void rollback();

  /**
   * Closes and releases this connection.
   */
  void close();
}
