/*
 * DataHub.java
 * Copyright (C) 2022-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.internal.DataHubInner;
import com.github.sttk.sabi.internal.TxnFailureReportBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages local data sources and data connections, and executes business logic in transactional or
 * non-transactional scopes.
 *
 * <p>A {@code DataHub} acts as a container for local {@link DataSrc} instances and provides access
 * to connections implementing {@link DataConn}. It supports executing logic functions through
 * {@link #run(Logic)} for non-transactional operations and {@link #txn(Logic)} for transactional
 * operations. In a transaction, connection commits, rollbacks, and failure reporting are handled
 * automatically.
 */
public class DataHub implements DataAcc, AutoCloseable {

  /**
   * Represents an error when setting up global data sources fails.
   *
   * @param errors the list of error entries during global data source setup
   */
  public record FailToSetupGlobalDataSrcs(List<ErrEntry> errors) {}

  /**
   * Represents an error when setting up local data sources fails.
   *
   * @param errors the list of error entries during local data source setup
   */
  public record FailToSetupLocalDataSrcs(List<ErrEntry> errors) {}

  /**
   * Represents an error when no data source with the specified name is found.
   *
   * @param name the logical name of the data source
   * @param dataConnType the expected data connection class name
   */
  public record NoDataSrcToCreateDataConn(String name, String dataConnType) {}

  /**
   * Represents an error when creating a data connection fails.
   *
   * @param name the logical name of the data source
   * @param dataConnType the expected data connection class name
   */
  public record FailToCreateDataConn(String name, String dataConnType) {}

  /**
   * Represents an error when a created data connection instance is null.
   *
   * @param name the logical name of the data source
   * @param dataConnType the expected data connection class name
   */
  public record CreatedDataConnIsNull(String name, String dataConnType) {}

  /**
   * Represents an error when a connection cannot be cast to the target connection type.
   *
   * @param name the logical name of the data source
   * @param fromDataConnType the actual connection class name
   * @param toDataConnType the target connection class name
   */
  public record FailToCastDataConn(String name, String fromDataConnType, String toDataConnType) {}

  /**
   * Represents an error when casting this {@code DataHub} instance to the generic data context
   * fails.
   *
   * @param fromType the class name of this DataHub instance
   */
  public record FailToCastDataHub(String fromType) {}

  /** Represents an error when an unhandled runtime exception occurs during logic execution. */
  public record RuntimeExceptionOccurred() {}

  ///

  private final DataHubInner inner;

  /** Constructs a new, default {@code DataHub} instance. */
  public DataHub() {
    this.inner = new DataHubInner();
  }

  /**
   * Constructs a new {@code DataHub} instance configured to use specified global data sources.
   *
   * @param names the list of global data source names to bind to this hub
   */
  public DataHub(List<String> names) {
    this.inner = new DataHubInner(names);
  }

  /**
   * Constructs a new {@code DataHub} instance configured to use specified global data sources.
   *
   * @param names varargs array of global data source names to bind to this hub
   */
  public DataHub(String... names) {
    this(List.of(names));
  }

  /**
   * Registers a local {@link DataSrc} with a specific name in this hub.
   *
   * @param name the logical name for the data source
   * @param ds the {@link DataSrc} instance to register
   */
  public void uses(String name, DataSrc ds) {
    this.inner.useLocal(name, ds);
  }

  /**
   * Unregisters a local {@link DataSrc} associated with the given name from this hub.
   *
   * @param name the logical name of the data source to remove
   */
  public void disuses(String name) {
    this.inner.disuseLocal(name);
  }

  /** Closes all local data sources registered in this hub and releases their resources. */
  @Override
  public void close() {
    this.inner.closeLocals();
  }

  /**
   * {@inheritDoc}
   *
   * @throws Err if the data source is not found, connection creation fails or returns null, or
   *     casting to {@code cls} fails
   */
  @Override
  public <C extends DataConn> C getDataConn(String name, Class<C> cls) throws Err {
    return this.inner.getDataConn(name, cls);
  }

  /**
   * Executes business logic in a non-transactional scope.
   *
   * <p>This method passes this hub instance (cast to type {@code D}) to {@link Logic#run(Object)}.
   * Connections acquired during execution are managed within the scope.
   *
   * @param <D> the type of data context expected by the logic, typically {@code DataHub} or a
   *     subclass/interface
   * @param logic the business logic to execute
   * @throws Err if the logic throws {@code Err}, if casting to {@code D} fails (wrapping {@link
   *     FailToCastDataHub}), or if an unhandled runtime exception occurs (wrapping {@link
   *     RuntimeExceptionOccurred})
   */
  public <D> void run(Logic<D> logic) throws Err {
    try {
      @SuppressWarnings("unchecked")
      D data = (D) this;

      this.inner.begin();
      logic.run(data);
    } catch (Err err) {
      throw err;
    } catch (ClassCastException e) {
      throw new Err(new FailToCastDataHub(this.getClass().getName()), e);
    } catch (RuntimeException re) {
      throw new Err(new RuntimeExceptionOccurred(), re);
    } finally {
      this.inner.end();
    }
  }

  /**
   * Executes business logic within a transactional boundary.
   *
   * <p>Upon successful completion of {@link Logic#run(Object)}, all acquired data connections are
   * committed. If an exception occurs, all connections are automatically rolled back, transaction
   * failure reports are dispatched to connections, and the exception is rethrown wrapped in {@link
   * Err}.
   *
   * @param <D> the type of data context expected by the logic
   * @param logic the business logic to execute transactionally
   * @throws Err if the logic throws {@code Err}, connection commit or rollback fails, casting to
   *     {@code D} fails, or a runtime exception occurs
   */
  public <D> void txn(Logic<D> logic) throws Err {
    final var reportBuilders = new ArrayList<TxnFailureReportBuilder>(0);
    try {
      try {
        @SuppressWarnings("unchecked")
        D data = (D) this;

        this.inner.begin();
        logic.run(data);
      } finally {
        this.inner.prepareTxnFailureReportBuilders(reportBuilders);
      }

      this.inner.commit(reportBuilders);
    } catch (Err err) {
      this.inner.rollback(reportBuilders);
      throw err;
    } catch (ClassCastException e) {
      this.inner.rollback(reportBuilders);
      throw new Err(new FailToCastDataHub(this.getClass().getName()), e);
    } catch (RuntimeException re) {
      this.inner.rollback(reportBuilders);
      throw new Err(new RuntimeExceptionOccurred(), re);
    } finally {
      this.inner.end();
    }
  }
}
