/*
 * DataSrcContainer.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

class DataSrcContainer {
  final boolean local;
  final String name;
  final DataSrc ds;

  DataSrcContainer(boolean local, String name, DataSrc ds) {
    this.local = local;
    this.name = name;
    this.ds = ds;
  }
}
