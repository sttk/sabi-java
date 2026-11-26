/*
 * Globals.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages global {@link DataSrc} registrations and their lifecycle for the sabi framework.
 *
 * <p>Global data sources are registered via {@link #uses(String, DataSrc)} before calling {@link
 * #setup()} or {@link #setupWithOrder(String...)}. Once setup completes, the registered global
 * data sources are inherited by subsequently created {@link DataHub} instances until the {@link
 * AutoCloseable} returned by setup is closed.
 */
public final class Globals {
  private Globals() {}

  static final DataSrcManager DATA_SRC_MANAGER = new DataSrcManager(false);
  static final AtomicBoolean DATA_SRCS_FIXED = new AtomicBoolean(false);

  /**
   * Registers a global {@link DataSrc} under the specified name.
   *
   * <p>Registrations are ignored once {@link #setup()} or {@link #setupWithOrder(String...)} has
   * been invoked.
   *
   * @param name the logical name of the data source
   * @param ds the {@link DataSrc} instance to register
   */
  public static void uses(String name, DataSrc ds) {
    if (!DATA_SRCS_FIXED.get()) {
      DATA_SRC_MANAGER.add(name, ds);
    }
  }

  /**
   * Fixes global data source registrations and initializes all registered global {@link DataSrc}
   * instances in registration order.
   *
   * <p>If setup fails, any already initialized data sources are closed and an {@link Err} with
   * {@link DataHubError.FailToSetupGlobalDataSrcs} is thrown.
   *
   * @return an {@link AutoCloseable} that closes all global data sources when closed
   * @throws Err if setting up any global data source fails
   */
  public static AutoCloseable setup() throws Err {
    if (DATA_SRCS_FIXED.compareAndSet(false, true)) {
      var errors = DATA_SRC_MANAGER.setup();
      if (!errors.isEmpty()) {
        DATA_SRC_MANAGER.close();
        throw new Err(new DataHubError.FailToSetupGlobalDataSrcs(errors));
      }
    }
    return new AutoShutdown();
  }

  /**
   * Fixes global data source registrations and initializes all registered global {@link DataSrc}
   * instances, prioritizing the specified setup order.
   *
   * <p>Data sources matching {@code names} are initialized first in the given order (with earlier
   * names taking precedence if duplicates appear), followed by any remaining registered data sources.
   *
   * @param names the ordered logical names of global data sources to initialize first
   * @return an {@link AutoCloseable} that closes all global data sources when closed
   * @throws Err if setting up any global data source fails
   */
  public static AutoCloseable setupWithOrder(String... names) throws Err {
    if (DATA_SRCS_FIXED.compareAndSet(false, true)) {
      var errors = DATA_SRC_MANAGER.setupWithOrder(List.of(names));
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
