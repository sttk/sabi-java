/*
 * Dax class.
 * Copyright (C) 2022-2023 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.sabi.errs.Err;

/**
 * {@code Dax} is the interface for a set of data access methods.
 *
 * This interface is inherited by dax implementations for data stores, and
 * each dax implementation defines data access methods to each data store.
 * In data access methods, {@link DaxConn} instances conected to data stores
 * can be obtained with {@link #getDaxConn} method.
 */
public interface Dax {

  /**
   * Gets a {@link DaxConn} instance associated with the argument name.
   * The name is same as what was registered with {@link DaxSrc} using
   * {@link Sabi#uses} method.
   *
   * @param <C>  The type of a {@link DaxConn}.
   * @param name  A name of a {@link DaxConn}.
   * @return  A {@link DaxConn} instance.
   * @throws Err  If getting a {@link DaxConn} fails.
   */
  <C extends DaxConn> C getDaxConn(String name) throws Err;
}
