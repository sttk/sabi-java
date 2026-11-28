/*
 * DataRun.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

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

    try {
      this.localDataSrcManager.remove(name);
    } finally {
      this.dataSrcMap.computeIfPresent(
          name,
          (nm, cont) -> {
            return cont.local ? Globals.DATA_SRC_MANAGER.getReadyDs(name) : cont;
          });
    }
  }

  void close() {
    Throwable firstThrowable = null;
    try {
      this.dataConnManager.close();
    } catch (Throwable t) {
      firstThrowable = t;
    }

    try {
      this.localDataSrcManager.close();
    } catch (Throwable t) {
      if (firstThrowable == null) {
        firstThrowable = t;
      } else {
        firstThrowable.addSuppressed(t);
      }
    } finally {
      this.dataSrcMap.clear();
    }

    if (firstThrowable != null) {
      if (firstThrowable instanceof RuntimeException) {
        throw RuntimeException.class.cast(firstThrowable);
      } else {
        throw Error.class.cast(firstThrowable);
      }
    }
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
    try {
      this.dataConnManager.close();
      this.fixed = false;
    } catch (Throwable t) {
      this.dataConnManager.abend();
      throw t;
    }
  }

  void abend() {
    this.localDataSrcManager.isAbnormal = true;
    this.dataConnManager.abend();
    this.dataSrcMap.clear();
    try {
      this.localDataSrcManager.close();
    } catch (Throwable t) {
      // no throw
    }
  }

  <C extends DataConn> C getDataConn(String name, Class<C> cls) throws Err {
    final var dsMap = this.dataSrcMap;

    return this.dataConnManager.getOrAdd(
        name,
        cls,
        () -> {
          var dsCont = dsMap.get(name);
          if (dsCont == null) {
            throw new Err(new DataHubError.NoDataSrcToCreateDataConn(name, cls.getName()));
          }
          return dsCont.createDataConn(cls);
        });
  }
}
