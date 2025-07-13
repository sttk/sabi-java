/*
 * DataHub class.
 * Copyright (C) 2022-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.internal.DataHubInner;
import java.util.Map;

/**
 * {@code DataHub} is a central component in the Sabi framework that manages {@link DataSrc} and
 * {@link DataConn} instances, facilitating data access and transaction management. It implements
 * both {@link DataAcc} for data access operations and {@link AutoCloseable} for resource
 * management.
 *
 * <p>This class allows for the registration and unregistration of local {@link DataSrc} objects,
 * and provides methods to execute application logic with or without transactional boundaries.
 */
public class DataHub implements DataAcc, AutoCloseable {
  /**
   * Represents an error reason that occurred when failing to set up global {@link DataSrc}
   * instances.
   *
   * @param errors A map containing the names of the data sources and the corresponding exceptions
   *     that occurred during their setup.
   */
  public record FailToSetupGlobalDataSrcs(Map<String, Exc> errors) {}

  /**
   * Represents an error reason that occurred when failing to set up local {@link DataSrc}
   * instances.
   *
   * @param errors A map containing the names of the data sources and the corresponding exceptions
   *     that occurred during their setup.
   */
  public record FailToSetupLocalDataSrcs(Map<String, Exc> errors) {}

  /**
   * Represents an error reason that occurred when failing to commit one or more {@link DataConn}
   * instances.
   *
   * @param errors A map containing the names of the data connections and the corresponding
   *     exceptions that occurred during their commit.
   */
  public record FailToCommitDataConn(Map<String, Exc> errors) {}

  /**
   * Represents an error reason that occurred when failing to pre-commit one or more {@link
   * DataConn} instances.
   *
   * @param errors A map containing the names of the data connections and the corresponding
   *     exceptions that occurred during their pre-commit.
   */
  public record FailToPreCommitDataConn(Map<String, Exc> errors) {}

  /**
   * Represents an error reason where no {@link DataSrc} was found to create a {@link DataConn} with
   * the specified name and type.
   *
   * @param name The name of the data source requested.
   * @param dataConnType The type of the data connection requested.
   */
  public record NoDataSrcToCreateDataConn(String name, String dataConnType) {}

  /**
   * Represents an error reason that occurred when failing to create a {@link DataConn} instance.
   *
   * @param name The name of the data source from which the connection was attempted.
   * @param dataConnType The type of the data connection that failed to be created.
   */
  public record FailToCreateDataConn(String name, String dataConnType) {}

  /**
   * Represents an error reason where the created {@link DataConn} instance was null.
   *
   * @param name The name of the data source.
   * @param dataConnType The type of the data connection expected.
   */
  public record CreatedDataConnIsNull(String name, String dataConnType) {}

  /**
   * Represents an error reason that occurred when failing to cast a {@link DataConn} to the
   * requested type.
   *
   * @param name The name of the data connection.
   * @param castToType The type to which the data connection was attempted to be cast.
   */
  public record FailToCastDataConn(String name, String castToType) {}

  /**
   * Represents an error reason that occurred when failing to cast the {@code DataHub} instance
   * itself to the expected data access interface type for a {@link Logic}.
   *
   * @param castFromType The actual type of the {@code DataHub} instance that failed to cast.
   */
  public record FailToCastDataHub(String castFromType) {}

  /**
   * Represents an unexpected {@link RuntimeException} that occurred during pre-commit or commit
   * operations.
   */
  public record RuntimeExceptionOccurred() {}

  private final DataHubInner inner = new DataHubInner();

  /** Constructs a new {@code DataHub} instance. */
  public DataHub() {}

  /**
   * Registers a local {@link DataSrc} with the specified name for use within this {@code DataHub}
   * instance. This allows specific data sources to be managed independently from globally
   * registered ones.
   *
   * @param name The unique name for the {@link DataSrc}.
   * @param ds The {@link DataSrc} instance to register.
   */
  public void uses(String name, DataSrc ds) {
    inner.uses(name, ds);
  }

  /**
   * Unregisters a local {@link DataSrc} with the given name from this {@code DataHub} instance.
   *
   * @param name The name of the {@link DataSrc} to unregister.
   */
  public void disuses(String name) {
    inner.disuses(name);
  }

  /**
   * Executes the provided application {@link Logic} without transactional boundaries. The {@code
   * DataHub} instance itself is passed as the data access object {@code D} to the {@link Logic}'s
   * {@code run} method.
   *
   * @param <D> The type of the data access object, which typically is {@code DataHub} or an
   *     interface implemented by {@code DataHub} that {@link Logic} expects.
   * @param logic The application logic to execute.
   * @throws Exc if an {@link Exc} or {@link RuntimeException} occurs during logic execution or if
   *     the {@code DataHub} cannot be cast to the expected data access type.
   */
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

  /**
   * Executes the provided application {@link Logic} within a transactional context. The {@code
   * DataHub} instance is passed as the data access object {@code D} to the {@link Logic}'s {@code
   * run} method. If the logic completes successfully, a commit operation is attempted. If any
   * {@link Exc}, {@link RuntimeException}, or {@link Error} occurs, a rollback operation is
   * performed.
   *
   * @param <D> The type of the data access object, which typically is {@code DataHub} or an
   *     interface implemented by {@code DataHub} that {@link Logic} expects.
   * @param logic The application logic to execute transactionally.
   * @throws Exc if an {@link Exc}, {@link RuntimeException}, or {@link Error} occurs during logic
   *     execution, pre-commit, or commit. The original exception is re-thrown after rollback.
   */
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

  /**
   * Retrieves a {@link DataConn} instance from the managed data sources. This method is part of the
   * {@link DataAcc} interface implementation.
   *
   * @param <C> The type of the {@link DataConn} to retrieve, which must extend {@link DataConn}.
   * @param name The name of the data source from which to get the connection.
   * @param cls The {@link Class} object representing the desired type of the data connection.
   * @return A {@link DataConn} instance of the specified type.
   * @throws Exc if no data source is found, if the connection cannot be created, if the created
   *     connection is null, or if the connection cannot be cast to the specified class.
   */
  @Override
  public <C extends DataConn> C getDataConn(String name, Class<C> cls) throws Exc {
    return inner.getDataConn(name, cls);
  }

  /**
   * Closes all {@link DataConn} instances managed by this {@code DataHub}, releasing their
   * resources. This method is part of the {@link AutoCloseable} interface and should be called to
   * ensure proper resource cleanup, ideally in a try-with-resources statement.
   */
  @Override
  public void close() {
    inner.close();
  }
}
