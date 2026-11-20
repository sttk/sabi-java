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
    this.local = local;
    this.name = name;
    this.ds = ds;
  }

  <C extends DataConn> C createDataConn(Class<C> cls) throws Err {
    DataConn dc;
    try {
      dc = this.ds.createDataConn();
    } catch (Exception e) {
      throw new Err(new DataSrcError.FailToCreateDataConn(name, cls.getName()), e);
    }
    if (dc == null) {
      throw new Err(new DataSrcError.CreateDataConnIsNull(name, cls.getName()));
    }

    try {
      return cls.cast(dc);
    } catch (Exception e) {
      var fromType = dc.getClass().getName();
      var toType = cls.getName();
      throw new Err(new DataSrcError.FailToCastCreatedDataConn(name, fromType, toType), e);
    }
  }

  static boolean isNoDataSrc(DataSrcContainer cont) {
    return (cont == null || cont.ds == null);
  }
}
