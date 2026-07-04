/*
 * DataSrcContainer.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import com.github.sttk.sabi.DataSrc;

public class DataSrcContainer {
  final boolean local;
  final String name;
  DataSrc ds;

  DataSrcContainer(boolean local, String name, DataSrc ds) {
    this.local = local;
    this.name = name;
    this.ds = ds;
  }
}
