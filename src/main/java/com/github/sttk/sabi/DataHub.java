// Copyright (C) 2024-2026 Takayuki Sato. All Rights Reserved.
// This program is free software under MIT License.
// See the file LICENSE in this distribution for more details.

package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.List;

/**
 * Manages data sources and data connections for executing business logic.
 *
 * <p>A {@code DataHub} inherits global {@link DataSrc} instances registered via {@link
 * Globals#uses(String, DataSrc)} and manages session-local {@link DataSrc} and {@link DataConn}
 * instances. Subclasses typically implement application-specific data access interfaces alongside
 * {@link DataAcc} so that {@link Logic} instances can access required operations without depending
 * on concrete data sources.
 */
public class DataHub implements DataAcc, AutoCloseable {

  final DataRun run;

  /** Constructs a new {@code DataHub} inheriting ready global data sources. */
  public DataHub() {
    this.run = new DataRun();
  }

  /**
   * Constructs a new {@code DataHub} with a predefined order for managing named data connections.
   *
   * <p>When data connections are committed or closed, connections matching {@code names} follow the
   * specified ordering (or reverse order when closing). If duplicate names are provided, earlier
   * entries take precedence.
   *
   * @param names the ordered logical names of data connections
   */
  public DataHub(String... names) {
    this.run = new DataRun(List.of(names));
  }

  /** Closes all managed data connections and local data sources held by this {@code DataHub}. */
  @Override
  public void close() {
    this.run.close();
  }

  /**
   * Retrieves a data connection associated with the specified data source name and casts it to the
   * requested connection class type.
   *
   * @param <C> the expected type of {@link DataConn}
   * @param name the registered logical name of the data source
   * @param cls the {@link Class} representing the target connection type {@code C}
   * @return the data connection instance associated with the specified name
   * @throws Err if no data source is available for {@code name}, if creating the connection fails
   *     or returns {@code null}, or if the connection cannot be cast to {@code cls}
   */
  @Override
  public final <C extends DataConn> C getDataConn(String name, Class<C> cls) throws Err {
    return this.run.getDataConn(name, cls);
  }

  /**
   * Executes the specified {@link Logic} using this {@code DataHub} as the data access instance.
   *
   * @param <D> the type of data access interface expected by the logic
   * @param logic the logic unit to execute
   * @throws Err if an error occurs during logic execution
   */
  @SuppressWarnings("unchecked")
  @Override
  public final <D> void run(Logic<D> logic) throws Err {
    logic.run((D) this);
  }

  /**
   * Starts a nested {@link LogicRun} chain using this {@code DataHub}.
   *
   * @return a new nested {@link LogicRun} instance
   */
  @Override
  public final LogicRun start() {
    return new LogicRun(this, true);
  }
}
