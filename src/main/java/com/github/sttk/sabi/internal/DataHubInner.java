/*
 * DataHubInner class.
 * Copyright (C) 2022-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.DataHub;
import com.github.sttk.sabi.DataSrc;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataHubInner {

  static final DataSrcList GLOBAL_DATA_SRC_LIST = new DataSrcList(false);
  static AtomicBoolean GLOBAL_DATA_SRCS_FIXED = new AtomicBoolean(false);

  public static void usesGlobal(String name, DataSrc ds) {
    if (!GLOBAL_DATA_SRCS_FIXED.get()) {
      GLOBAL_DATA_SRC_LIST.addDataSrc(name, ds);
    }
  }

  public static AutoCloseable setupGlobals() throws Exc {
    if (GLOBAL_DATA_SRCS_FIXED.compareAndSet(false, true)) {
      var excMap = GLOBAL_DATA_SRC_LIST.setupDataSrcs();
      if (!excMap.isEmpty()) {
        GLOBAL_DATA_SRC_LIST.closeDataSrcs();
        throw new Exc(new DataHub.FailToSetupGlobalDataSrcs(excMap));
      }
    }
    return new AutoShutdown();
  }

  static class AutoShutdown implements AutoCloseable {
    @Override
    public void close() {
      GLOBAL_DATA_SRC_LIST.closeDataSrcs();
    }
  }

  final DataSrcList localDataSrcList = new DataSrcList(true);
  final Map<String, DataSrcContainer> dataSrcMap = new HashMap<>();
  final DataConnList dataConnList = new DataConnList();
  final Map<String, DataConnContainer> dataConnMap = new HashMap<>();
  boolean fixed = false;

  public DataHubInner() {
    GLOBAL_DATA_SRCS_FIXED.compareAndSet(false, true);
    GLOBAL_DATA_SRC_LIST.copyContainerPtrsDidSetupInto(this.dataSrcMap);
  }

  public void uses(String name, DataSrc ds) {
    if (this.fixed) {
      return;
    }

    this.localDataSrcList.addDataSrc(name, ds);
  }

  public void disuses(String name) {
    if (this.fixed) {
      return;
    }

    var ptr = this.dataSrcMap.get(name);
    if (ptr != null && ptr.local && Objects.equals(ptr.name, name)) {
      this.dataSrcMap.remove(name);
    }

    this.localDataSrcList.removeAndCloseContainerPtrDidSetupByName(name);
    this.localDataSrcList.removeAndCloseContainerPtrNotSetupByName(name);
  }

  public void close() {
    this.dataConnMap.clear();
    this.dataConnList.closeDataConns();

    this.dataSrcMap.clear();
    this.localDataSrcList.closeDataSrcs();
  }

  public void begin() throws Exc {
    this.fixed = true;

    var excMap = this.localDataSrcList.setupDataSrcs();
    this.localDataSrcList.copyContainerPtrsDidSetupInto(this.dataSrcMap);

    if (!excMap.isEmpty()) {
      throw new Exc(new DataHub.FailToSetupLocalDataSrcs(excMap));
    }
  }

  public void commit() throws Exc {
    var excMap = new HashMap<String, Exc>();

    var ag = new AsyncGroupImpl();
    var ptr = this.dataConnList.head;
    while (ptr != null) {
      ag.name = ptr.name;
      try {
        ptr.conn.preCommit(ag);
      } catch (Exc e) {
        excMap.put(ptr.name, e);
        break;
      } catch (RuntimeException e) {
        excMap.put(ptr.name, new Exc(new DataHub.RuntimeExceptionOccurred(), e));
        break;
      }
      ptr = ptr.next;
    }
    ag.joinAndPutExcsInto(excMap);

    if (!excMap.isEmpty()) {
      throw new Exc(new DataHub.FailToPreCommitDataConn(excMap));
    }

    ag = new AsyncGroupImpl();
    ptr = this.dataConnList.head;
    while (ptr != null) {
      ag.name = ptr.name;
      try {
        ptr.conn.commit(ag);
      } catch (Exc e) {
        excMap.put(ptr.name, e);
        break;
      } catch (RuntimeException e) {
        excMap.put(ptr.name, new Exc(new DataHub.RuntimeExceptionOccurred(), e));
        break;
      }
      ptr = ptr.next;
    }
    ag.joinAndPutExcsInto(excMap);

    if (!excMap.isEmpty()) {
      throw new Exc(new DataHub.FailToCommitDataConn(excMap));
    }
  }

  public void rollback() {
    var ag = new AsyncGroupImpl();
    var ptr = this.dataConnList.head;
    while (ptr != null) {
      ag.name = ptr.name;
      if (ptr.conn.shouldForceBack()) {
        ptr.conn.forceBack(ag);
      } else {
        ptr.conn.rollback(ag);
      }
      ptr = ptr.next;
    }

    ag.joinAndIgnoreExcs();
  }

  public void postCommit() {
    var ag = new AsyncGroupImpl();
    var ptr = this.dataConnList.head;
    while (ptr != null) {
      ag.name = ptr.name;
      ptr.conn.postCommit(ag);
      ptr = ptr.next;
    }

    ag.joinAndIgnoreExcs();
  }

  public void end() {
    this.dataConnMap.clear();
    this.dataConnList.closeDataConns();
    this.fixed = false;
  }

  public <C extends DataConn> C getDataConn(String name, Class<C> cls) throws Exc {
    var connPtr = this.dataConnMap.get(name);
    if (connPtr != null) {
      try {
        return cls.cast(connPtr.conn);
      } catch (Exception e) {
        throw new Exc(new DataHub.FailToCastDataConn(name, cls.getName()), e);
      }
    }

    var dsPtr = this.dataSrcMap.get(name);
    if (dsPtr == null) {
      throw new Exc(new DataHub.NoDataSrcToCreateDataConn(name, cls.getName()));
    }

    DataConn conn;
    try {
      conn = dsPtr.ds.createDataConn();
    } catch (Exc | RuntimeException e) {
      throw new Exc(new DataHub.FailToCreateDataConn(name, cls.getName()));
    }
    if (conn == null) {
      throw new Exc(new DataHub.CreatedDataConnIsNull(name, cls.getName()));
    }

    C c;
    try {
      c = cls.cast(conn);
    } catch (Exception e) {
      throw new Exc(new DataHub.FailToCastDataConn(name, cls.getName()), e);
    }

    connPtr = new DataConnContainer(name, c);
    this.dataConnMap.put(name, connPtr);
    this.dataConnList.appendContainer(connPtr);

    return c;
  }
}
