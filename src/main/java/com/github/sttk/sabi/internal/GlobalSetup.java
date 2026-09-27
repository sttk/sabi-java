/*
 * GlobalSetup.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import static com.github.sttk.sabi.Sabi.FailToSetupGlobalDataSrcs;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.DataSrc;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GlobalSetup {
  static final DataSrcManager GLOBAL_DATA_SRC_MANAGER = new DataSrcManager(false);
  static final AtomicBoolean GLOBAL_DATA_SRCS_FIXED = new AtomicBoolean(false);

  public static void uses(String name, DataSrc ds) {
    if (!GLOBAL_DATA_SRCS_FIXED.get()) {
      GLOBAL_DATA_SRC_MANAGER.add(name, ds);
    }
  }

  public static AutoCloseable setup() throws Err {
    if (GLOBAL_DATA_SRCS_FIXED.compareAndSet(false, true)) {
      var errors = GLOBAL_DATA_SRC_MANAGER.setup();
      if (!errors.isEmpty()) {
        GLOBAL_DATA_SRC_MANAGER.close();
        throw new Err(new FailToSetupGlobalDataSrcs(errors));
      }
    }
    return new AutoShutdown();
  }

  public static AutoCloseable setupWithOrder(List<String> names) throws Err {
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
}
