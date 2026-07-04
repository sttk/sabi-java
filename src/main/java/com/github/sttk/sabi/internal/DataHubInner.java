/*
 * DataHubInner.java
 * Copyright (C) 2022-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import static com.github.sttk.sabi.DataHub.CreatedDataConnIsNull;
import static com.github.sttk.sabi.DataHub.FailToCastDataConn;
import static com.github.sttk.sabi.DataHub.FailToCreateDataConn;
import static com.github.sttk.sabi.DataHub.FailToSetupGlobalDataSrcs;
import static com.github.sttk.sabi.DataHub.FailToSetupLocalDataSrcs;
import static com.github.sttk.sabi.DataHub.NoDataSrcToCreateDataConn;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.DataSrc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DataHubInner {
  static final DataSrcManager GLOBAL_DATA_SRC_MANAGER = new DataSrcManager(false);
  static final AtomicBoolean GLOBAL_DATA_SRCS_FIXED = new AtomicBoolean(false);

  public static void useGlobal(String name, DataSrc ds) {
    if (!GLOBAL_DATA_SRCS_FIXED.get()) {
      GLOBAL_DATA_SRC_MANAGER.add(name, ds);
    }
  }

  public static AutoCloseable setupGlobals() throws Err {
    if (GLOBAL_DATA_SRCS_FIXED.compareAndSet(false, true)) {
      var errors = GLOBAL_DATA_SRC_MANAGER.setup();
      if (!errors.isEmpty()) {
        GLOBAL_DATA_SRC_MANAGER.close();
        throw new Err(new FailToSetupGlobalDataSrcs(errors));
      }
    }
    return new AutoShutdown();
  }

  public static AutoCloseable setupGlobalsWithOrder(List<String> names) throws Err {
    if (GLOBAL_DATA_SRCS_FIXED.compareAndSet(false, true)) {
      var errors = GLOBAL_DATA_SRC_MANAGER.setupWithOrder(names);
      if (!errors.isEmpty()) {
        GLOBAL_DATA_SRC_MANAGER.close();
        throw new Err(new FailToSetupGlobalDataSrcs(errors));
      }
    }
    return new AutoShutdown();
  }

  static class AutoShutdown implements AutoCloseable {
    @Override
    public void close() {
      GLOBAL_DATA_SRC_MANAGER.close();
    }
  }

  final DataSrcManager localDataSrcManager;
  final Map<String, DataSrcContainer> dataSrcMap;
  final DataConnManager dataConnManager;
  final Map<String, DataConnContainer> dataConnMap;
  boolean fixed;

  public DataHubInner() {
    GLOBAL_DATA_SRCS_FIXED.compareAndSet(false, true);
    this.fixed = false;

    this.localDataSrcManager = new DataSrcManager(true);
    this.dataSrcMap = new HashMap<>();
    this.dataConnManager = new DataConnManager();
    this.dataConnMap = new HashMap<>();

    GLOBAL_DATA_SRC_MANAGER.copyDsReadyToMap(this.dataSrcMap);
  }

  public DataHubInner(List<String> names) {
    GLOBAL_DATA_SRCS_FIXED.compareAndSet(false, true);
    this.fixed = false;

    this.localDataSrcManager = new DataSrcManager(true);
    this.dataSrcMap = new HashMap<>();
    this.dataConnManager = new DataConnManager(names);
    this.dataConnMap = new HashMap<>();

    GLOBAL_DATA_SRC_MANAGER.copyDsReadyToMap(this.dataSrcMap);
  }

  public void useLocal(String name, DataSrc ds) {
    if (this.fixed) {
      return;
    }

    this.localDataSrcManager.add(name, ds);
  }

  public void disuseLocal(String name) {
    if (this.fixed) {
      return;
    }

    var cont = this.dataSrcMap.get(name);
    if (cont != null && cont.local && Objects.equals(cont.name, name)) {
      this.dataSrcMap.remove(name);
    }

    this.localDataSrcManager.remove(name);
  }

  public void closeLocals() {
    this.dataConnMap.clear();
    this.dataConnManager.close();

    this.dataSrcMap.clear();
    this.localDataSrcManager.close();
  }

  public void begin() throws Err {
    this.fixed = true;

    var errors = this.localDataSrcManager.setup();
    this.localDataSrcManager.copyDsReadyToMap(this.dataSrcMap);

    if (!errors.isEmpty()) {
      throw new Err(new FailToSetupLocalDataSrcs(errors));
    }
  }

  public void prepareTxnFailureReportBuilders(ArrayList<TxnFailureReportBuilder> list) {
    this.dataConnManager.prepareTxnFailureReportBuilders(list);
  }

  public void commit(List<TxnFailureReportBuilder> builders) throws Err {
    this.dataConnManager.commit(builders);
  }

  public void rollback(List<TxnFailureReportBuilder> builders) throws Err {
    this.dataConnManager.rollback(builders);
  }

  public void end() {
    this.dataConnMap.clear();
    this.dataConnManager.close();

    this.fixed = false;
  }

  public <C extends DataConn> C getDataConn(String name, Class<C> cls) throws Err {
    var dcCont = this.dataConnMap.get(name);
    if (dcCont != null && dcCont.conn != null) {
      try {
        return cls.cast(dcCont.conn);
      } catch (Exception e) {
        String fromType = dcCont.conn.getClass().getName();
        String toType = cls.getName();
        throw new Err(new FailToCastDataConn(name, fromType, toType), e);
      }
    }

    var dsCont = this.dataSrcMap.get(name);
    if (dsCont == null || dsCont.ds == null) {
      throw new Err(new NoDataSrcToCreateDataConn(name, cls.getName()));
    }

    DataConn dc;
    try {
      dc = dsCont.ds.createDataConn();
    } catch (Exception e) {
      throw new Err(new FailToCreateDataConn(name, cls.getName()), e);
    }
    if (dc == null) {
      throw new Err(new CreatedDataConnIsNull(name, cls.getName()));
    }

    dcCont = new DataConnContainer(name, dc);
    this.dataConnMap.put(name, dcCont);
    this.dataConnManager.add(dcCont);

    C c;
    try {
      c = cls.cast(dc);
    } catch (Exception e) {
      String fromType = dc.getClass().getName();
      String toType = cls.getName();
      throw new Err(new FailToCastDataConn(name, fromType, toType), e);
    }

    return c;
  }
}
