/*
 * DataConn class.
 * Copyright (C) 2022-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Exc;

public interface DataConn {
  void commit(AsyncGroup ag) throws Exc;
  default void preCommit(AsyncGroup ag) throws Exc {}
  default void postCommit(AsyncGroup ag) {}
  default boolean shouldForceBack() { return false; }
  void rollback(AsyncGroup ag);
  default void forceBack(AsyncGroup ag) {}
  void close();
}
