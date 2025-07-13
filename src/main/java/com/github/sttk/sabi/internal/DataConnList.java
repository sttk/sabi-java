/*
 * DataConnList class.
 * Copyright (C) 2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import com.github.sttk.errs.Exc;

public class DataConnList {
  DataConnContainer head;
  DataConnContainer last;

  public DataConnList() {}

  void appendContainer(DataConnContainer ptr) {
    ptr.next = null;

    if (this.last == null) {
      this.head = ptr;
      this.last = ptr;
      ptr.prev = null;
    } else {
      this.last.next = ptr;
      ptr.prev = this.last;
      this.last = ptr;
    }
  }

  void closeDataConns() {
    var ptr = this.last;
    while (ptr != null) {
      ptr.conn.close();
      ptr = ptr.prev;
    }
    this.head = null;
    this.last = null;
  }
}
