/*
 * DataConnContainer.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import com.github.sttk.sabi.DataConn;

public class DataConnContainer {
  final String name;
  final DataConn conn;

  DataConnContainer(String name, DataConn conn) {
    this.name = name;
    this.conn = conn;
  }
}
