/*
 * Sabi.java
 * Copyright (C) 2022-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.internal.DataHubInner;
import java.util.List;

/**
 * Utility class for managing and setting up global {@link DataSrc} instances.
 *
 * <p>Applications can register global data sources using {@link #uses(String, DataSrc)} during
 * startup, and then call {@link #setup()} or {@link #setup(String...)} to set them up. The returned
 * {@link AutoCloseable} handles shutting down and closing global data sources when the application
 * terminates or finishes using them.
 */
public final class Sabi {
  private Sabi() {}

  /**
   * Registers a global data source with the specified logical name.
   *
   * @param name the logical name for the global data source
   * @param ds the {@link DataSrc} instance to register
   */
  public static void uses(String name, DataSrc ds) {
    DataHubInner.useGlobal(name, ds);
  }

  /**
   * Sets up all registered global data sources in their registration order.
   *
   * @return an {@link AutoCloseable} that closes all initialized global data sources when closed
   * @throws Err if setting up any global data source fails
   */
  public static AutoCloseable setup() throws Err {
    return DataHubInner.setupGlobals();
  }

  /**
   * Sets up registered global data sources matching the specified names in the given order.
   *
   * @param names varargs array of global data source names to set up
   * @return an {@link AutoCloseable} that closes the set-up global data sources when closed
   * @throws Err if setting up any of the specified global data sources fails
   */
  public static AutoCloseable setup(String... names) throws Err {
    return DataHubInner.setupGlobalsWithOrder(List.of(names));
  }

  /**
   * Sets up registered global data sources matching the specified list of names in the given order.
   *
   * @param names list of global data source names to set up
   * @return an {@link AutoCloseable} that closes the set-up global data sources when closed
   * @throws Err if setting up any of the specified global data sources fails
   */
  public static AutoCloseable setup(List<String> names) throws Err {
    return DataHubInner.setupGlobalsWithOrder(names);
  }
}
