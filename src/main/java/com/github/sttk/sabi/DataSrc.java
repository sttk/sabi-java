/*
 * DataSrc.java
 * Copyright (C) 2022-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

public interface DataSrc {
  void setup(AsyncGroup ag) throws Err;

  void close();

  DataConn createDataConn() throws Err;
}
