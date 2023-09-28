/*
 * DaxConn class.
 * Copyright (C) 2022-2023 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.sabi.errs.Err;

/**
 * {@code DaxConn} is the interface that represents a connection to a data store.
 * This interface declares methods: {@link #commit}, {@link rollback} and
 * {@link #close} to
 * work in a transaction
 * process.
 *
 * {@link #commit} is the method for comming updates in a transaction.
 * {@link #rollback} is the method for rollback updates in a transaction.
 * If commiting and rollbacking procedures are asynchronous, the argument
 * {@link AsyncGroup}(s) are used to process them.
 * {@link #close} is the method to close this connection.
 */
public interface DaxConn {

  /**
   * Commits updates to a target data store in a transaction.
   * If the commit procedure is asynchronous, it is processed with an argument
   * {@link AsyncGroup} object.
   *
   * @param ag  An {@link AsyncGroup} object which is used if this commit
   *   procedure is asynchronous.
   * @throws Err  If commiting updates fails.
   */
  void commit(AsyncGroup ag) throws Err;

  /**
   * Checks whether updates are already committed.
   *
   * @return  true if committed, or no rollback mechanism.
   */
  default boolean isCommitted() {
    return true;
  }

  /**
   * Rollbacks updates to a target data store in a transaction.
   * If the rollback procedure is asynchronous, it is processed with an
   * argument {@link AsyncGroup} object.
   *
   * @param ag  An {@link AsyncGroup} object which is used if this rollback
   *   procedure is asynchronous.
   */
  default void rollback(AsyncGroup ag) {}

  /**
   * Reverts updates forcely even if updates are already commited or this
   * connection does not have rollback mechanism.
   *
   * @param ag  An {@link AsyncGroup} object which is used if this rollback
   *   procedure is asynchronous.
   */
  void forceBack(AsyncGroup ag);

  /**
   * Closes this connection.
   */
  void close();
}
