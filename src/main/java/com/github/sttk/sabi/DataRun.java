/*
 * DataRun.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import static com.github.sttk.sabi.DataSrcContainer.isNoDataSrc;

import com.github.sttk.errs.Err;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

final class DataRun {

  final DataSrcManager localDataSrcManager;
  final HashMap<String, DataSrcContainer> dataSrcMap;
  final DataConnManager dataConnManager;
  boolean fixed;

  DataRun() {
    this.fixed = false;

    var dsMap = new HashMap<String, DataSrcContainer>();
    Globals.DATA_SRC_MANAGER.copyDsReadyToMap(dsMap);

    this.localDataSrcManager = new DataSrcManager(true);
    this.dataSrcMap = dsMap;
    this.dataConnManager = new DataConnManager();
  }

  DataRun(List<String> names) {
    this.fixed = false;

    var dsMap = new HashMap<String, DataSrcContainer>();
    Globals.DATA_SRC_MANAGER.copyDsReadyToMap(dsMap);

    this.localDataSrcManager = new DataSrcManager(true);
    this.dataSrcMap = dsMap;
    this.dataConnManager = new DataConnManager(names);
  }

  boolean isAbnormal() {
    return this.localDataSrcManager.isAbnormal;
  }

  void uses(String name, DataSrc ds) {
    if (this.fixed) {
      return;
    }
    this.localDataSrcManager.add(name, ds);
  }

  void disuses(String name) {
    if (this.fixed) {
      return;
    }
    this.dataSrcMap.remove(name);
    this.localDataSrcManager.remove(name);
  }

  void close() {
    this.dataConnManager.close();
    this.dataSrcMap.clear();
    this.localDataSrcManager.close();
  }

  void begin() throws Err {
    this.fixed = true;

    var errors = this.localDataSrcManager.setup();
    if (errors.isEmpty()) {
      this.localDataSrcManager.copyDsReadyToMap(this.dataSrcMap);
    } else {
      this.fixed = false;
      throw new Err(new DataHubError.FailToSetupLocalDataSrcs(errors));
    }
  }

  ArrayList<TxnFailureReport.Builder> newTxnFailureReportBuilders(Err logicErr) {
    return this.dataConnManager.newTxnFailureReportBuilders(logicErr);
  }

  void commit(ArrayList<TxnFailureReport.Builder> builders) throws Err {
    this.dataConnManager.commit(builders);
  }

  void rollback(ArrayList<TxnFailureReport.Builder> builders) {
    this.dataConnManager.rollback(builders);
  }

  void end() {
    this.dataConnManager.close();
    this.fixed = false;
  }

  void abend() {
    this.localDataSrcManager.isAbnormal = true;
    this.dataConnManager.abend();
    this.dataSrcMap.clear();
    this.localDataSrcManager.close();
  }

  <C extends DataConn> C getDataConn(String name, Class<C> cls) throws Err {
    C dc = this.dataConnManager.findByNameAndCast(name, cls);
    if (dc != null) {
      return dc;
    }

    var dsCont = this.dataSrcMap.get(name);
    if (isNoDataSrc(dsCont)) {
      throw new Err(new DataHubError.NoDataSrcToCreateDataConn(name, cls.getName()));
    }

    dc = dsCont.createDataConn(cls);
    var dcCont = new DataConnContainer(name, dc);
    this.dataConnManager.add(dcCont);
    return dc;
  }
}
