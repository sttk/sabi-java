/*
 * DataAcc.java
 * Copyright (C) 2023-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

public interface DataAcc {
  <C extends DataConn> C getDataConn(String name, Class<C> cls) throws Err;

  <D> void run(Logic<D> logic) throws Err;

  LogicRun start();
}
