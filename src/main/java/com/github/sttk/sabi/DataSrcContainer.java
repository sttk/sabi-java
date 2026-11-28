/*
 * DataSrcContainer.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

class DataSrcContainer {
  final boolean local;
  final String name;
  final DataSrc ds;

  DataSrcContainer(boolean local, String name, DataSrc ds) {
    if (name == null) throw new NullPointerException("name");
    if (ds == null) throw new NullPointerException("DataSrc");

    this.local = local;
    this.name = name;
    this.ds = ds;
  }

  <C extends DataConn> C createDataConn(Class<C> cls) throws Err {
    DataConn dc;
    try {
      dc = this.ds.createDataConn();
    } catch (Err e) {
      throw new Err(new DataSrcError.FailToCreateDataConn(name, cls.getName()), e);
    }
    if (dc == null) {
      throw new Err(new DataSrcError.CreateDataConnIsNull(name, cls.getName()));
    }

    try {
      return cls.cast(dc);
    } catch (ClassCastException e) {
      var fromType = dc.getClass().getName();
      var toType = cls.getName();
      var err = new Err(new DataSrcError.FailToCastCreatedDataConn(name, fromType, toType), e);
      try {
        dc.close();
      } catch (RuntimeException | Error ee) {
        ee.addSuppressed(err);
        throw ee;
      }
      throw err;
    }
  }
}
