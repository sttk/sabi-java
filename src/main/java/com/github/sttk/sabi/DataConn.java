/*
 * DataConn.java
 * Copyright (C) 2022-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Exc;

/**
 * The interface that abstracts a connection per session to an external data service, such as a
 * database, file system, or messaging service.
 *
 * <p>Its primary purpose is to enable cohesive transaction operations across multiple external data
 * services within a single transaction context. Implementations of this interface provide the
 * concrete input/output operations for their respective data services.
 *
 * <p>Methods declared within this interface are designed to handle transactional logic. The
 * AsyncGroup parameter in various methods allows for asynchronous processing when commit or
 * rollback operations are time-consuming.
 */
public interface DataConn {
  /**
   * Commits the changes made within the current session to the external data service. This method
   * is responsible for finalizing all operations performed since the last commit or rollback.
   *
   * @param ag An {@link AsyncGroup} that can be used to perform asynchronous operations if the
   *     commit process is time-consuming.
   * @throws Exc if an error occurs during the commit operation.
   */
  void commit(AsyncGroup ag) throws Exc;

  /**
   * Performs any necessary pre-commit operations. This method is called before the {@link
   * #commit(AsyncGroup)} method.
   *
   * @param ag An {@link AsyncGroup} that can be used for asynchronous pre-commit tasks.
   * @throws Exc if an error occurs during the pre-commit operation.
   */
  default void preCommit(AsyncGroup ag) throws Exc {}

  /**
   * Performs any necessary post-commit operations. This method is called after the {@link
   * #commit(AsyncGroup)} method has successfully completed.
   *
   * @param ag An {@link AsyncGroup} that can be used for asynchronous post-commit tasks.
   */
  default void postCommit(AsyncGroup ag) {}

  /**
   * Indicates whether a force-back operation should be performed. A force-back is a mechanism to
   * revert the committed changes when this connection had been already committed but the other
   * connection had failed.
   *
   * @return {@code true} if a force-back is required, {@code false} otherwise.
   */
  default boolean shouldForceBack() {
    return false;
  }

  /**
   * Rolls back the changes made within the current session, discarding all operations performed
   * since the last commit or rollback.
   *
   * @param ag An {@link AsyncGroup} that can be used to perform asynchronous operations if the
   *     rollback process is time-consuming.
   */
  void rollback(AsyncGroup ag);

  /**
   * Performs a force-back operation to revert the committed changes when this connection had been
   * already committed but the other connection had failed.
   *
   * @param ag An {@link AsyncGroup} that can be used for asynchronous force-back tasks.
   */
  default void forceBack(AsyncGroup ag) {}

  /**
   * Closes the connection to the external data service, releasing any associated resources. This
   * method should be called to ensure proper resource management.
   */
  void close();
}
