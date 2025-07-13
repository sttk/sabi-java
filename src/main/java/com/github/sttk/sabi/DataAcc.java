/*
 * DataAcc class.
 * Copyright (C) 2023-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Exc;

public interface DataAcc {
  <C extends DataConn> C getDataConn(String name, Class<C> cls) throws Exc;
}
