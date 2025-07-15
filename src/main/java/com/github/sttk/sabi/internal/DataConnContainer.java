/*
 * DataConnContainer.java
 * Copyright (C) 2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import com.github.sttk.sabi.DataConn;

public class DataConnContainer {
  DataConnContainer prev;
  DataConnContainer next;
  String name;
  DataConn conn;

  DataConnContainer(String name, DataConn conn) {
    this.name = name;
    this.conn = conn;
  }
}
