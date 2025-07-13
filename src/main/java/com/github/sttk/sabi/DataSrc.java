/*
 * DataSrc class.
 * Copyright (C) 2022-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Exc;

public interface DataSrc {
  void setup(AsyncGroup ag) throws Exc;

  void close();

  DataConn createDataConn() throws Exc;
}
