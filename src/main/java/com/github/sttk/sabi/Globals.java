/*
 * Globals.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Globals {
  private Globals() {}

  static final DataSrcManager DATA_SRC_MANAGER = new DataSrcManager(false);
  static final AtomicBoolean DATA_SRCS_FIXED = new AtomicBoolean(false);

  public static void uses(String name, DataSrc ds) {
    if (!DATA_SRCS_FIXED.get()) {
      DATA_SRC_MANAGER.add(name, ds);
    }
  }

  public static AutoCloseable setup() throws Err {
    if (!DATA_SRCS_FIXED.compareAndSet(false, true)) {
      var errors = DATA_SRC_MANAGER.setup();
      if (!errors.isEmpty()) {
        DATA_SRC_MANAGER.close();
        throw new Err(new DataHubError.FailToSetupGlobalDataSrcs(errors));
      }
    }
    return new AutoShutdown();
  }

  public static AutoCloseable setupWithOrder(List<String> names) throws Err {
    if (!DATA_SRCS_FIXED.compareAndSet(false, true)) {
      var errors = DATA_SRC_MANAGER.setupWithOrder(names);
      if (!errors.isEmpty()) {
        DATA_SRC_MANAGER.close();
        throw new Err(new DataHubError.FailToSetupGlobalDataSrcs(errors));
      }
    }
    return new AutoShutdown();
  }

  static class AutoShutdown implements AutoCloseable {
    @Override
    public void close() {
      DATA_SRC_MANAGER.close();
    }
  }
}
