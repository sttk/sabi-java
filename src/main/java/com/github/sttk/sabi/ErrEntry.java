/*
 * ErrEntry.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

public final class ErrEntry {

  public final int index;
  public final String name;
  public final Err err;

  public ErrEntry(int index, String name, Err err) {
    this.index = index;
    this.name = name;
    this.err = err;
  }
}
