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
 * #setup()} or {@link #setupWithOrder(String...)}. Once setup completes, the registered global data
 * sources are inherited by subsequently created {@link DataHub} instances until the {@link
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
   * been invoked. This method is not thread-safe: call it, together with {@code setup}, from a
   * single thread (typically the {@code main} method) before any other thread creates a {@link
   * DataHub}. Because the JVM loads classes lazily, calling this method from a static initializer
   * of another class may run it after {@code setup}, in which case the registration is silently
   * ignored.
   *
   * <p>If the same name is registered more than once, only the first registration is used. Later
   * ones are neither set up nor closed.
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
   * {@link DataHubError.FailToSetupGlobalDataSrcs} is thrown. Global setup cannot be retried after
   * a failure, and the global data sources are unavailable, so the application is expected to
   * terminate. Calling this method again after a failure does not throw and does not set anything
   * up.
   *
   * <p>Once setup completes, the global data sources are never modified, so they can be read by
   * multiple threads. Start any thread that creates a {@link DataHub} after this method returns.
   * This method itself is not thread-safe, and only the first call performs the setup; later calls
   * have no effect and return another handle that closes the same global data sources.
   *
   * <p>Close the returned {@link AutoCloseable} only after all {@link DataHub} instances and logic
   * executions have finished. Data sources are closed in the reverse order of their setup.
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
   * <p>Data sources whose names appear in {@code names} are initialized first in the given order,
   * followed by the remaining registered data sources in registration order. Duplicate names in
   * {@code names} are ignored (the first occurrence is used), and names with no registered data
   * source are skipped.
   *
   * <p>The failure handling, thread-safety, and closing rules are the same as those of {@link
   * #setup()}.
   *
   * @param names the ordered logical names of global data sources to initialize first; neither the
   *     array nor its elements may be {@code null}
   * @return an {@link AutoCloseable} that closes all global data sources when closed
   * @throws Err if setting up any global data source fails
   * @throws NullPointerException if {@code names} or any of its elements is {@code null}
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
    public void close() throws Exception {
      DATA_SRC_MANAGER.close();
    }
  }
}
