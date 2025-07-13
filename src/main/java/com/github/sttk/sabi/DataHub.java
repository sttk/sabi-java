/*
 * DataHub class.
 * Copyright (C) 2022-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.sabi.internal.DataHubInner;
import com.github.sttk.errs.Exc;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataHub implements DataAcc, AutoCloseable  {
  public record FailToSetupGlobalDataSrcs(Map<String, Exc> errors) {}
  public record FailToSetupLocalDataSrcs(Map<String, Exc> errors) {}
  public record FailToCommitDataConn(Map<String, Exc> errors) {}
  public record FailToPreCommitDataConn(Map<String, Exc> errors) {}
  public record NoDataSrcToCreateDataConn(String name, String dataConnType) {}
  public record FailToCreateDataConn(String name, String dataConnType) {}
  public record CreatedDataConnIsNull(String name, String dataConnType) {}
  public record FailToCastDataConn(String name, String castToType) {}
  public record FailToCastDataHub(String castFromType) {}
  public record RuntimeExceptionOccured() {}

  private final DataHubInner inner = new DataHubInner();

  public DataHub() {}

  public void uses(String name, DataSrc ds) {
    inner.uses(name, ds);
  }

  public void disuses(String name) {
    inner.disuses(name);
  }

  public <D> void run(Logic<D> logic) throws Exc {
    D data;
    try {
      @SuppressWarnings("unchecked")
      D d = (D) this;
      data = d;
    } catch (Exception e) {
      throw new Exc(new FailToCastDataHub(this.getClass().getName()));
    }
    try {
      inner.begin();
      logic.run(data);
    } catch (Exc | RuntimeException e) {
      throw e;
    } finally {
      inner.end();
    }
  }

  public <D> void txn(Logic<D> logic) throws Exc {
    D data;
    try {
      @SuppressWarnings("unchecked")
      D d = (D) this;
      data = d;
    } catch (Exception e) {
      throw new Exc(new FailToCastDataHub(this.getClass().getName()));
    }
    try {
      inner.begin();
      logic.run(data);
      inner.commit();
      inner.postCommit();
    } catch (Exc | RuntimeException | Error e) {
      inner.rollback();
      throw e;
    } finally {
      inner.end();
    }
  }

  @Override
  public <C extends DataConn> C getDataConn(String name, Class<C> cls) throws Exc {
    return inner.getDataConn(name, cls);
  }

  @Override
  public void close() {
    inner.close();
  }
}
