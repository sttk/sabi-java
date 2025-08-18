/*
 * Sabi.java
 * Copyright (C) 2022-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.internal.DataHubInner;

/**
 * {@code Sabi} is the class that provides the static methods related to the global functionalities
 * of sabi framework.
 *
 * <p>This class declares {@link #uses uses} method to register a {@link DataSrc} object used
 * globally with its name. And this class also declares {@link #setup setup} methods, which is the
 * static method to setup all global registered {@link DataSrc} objects.
 *
 * <p>The usages of these static methods is as follows:
 *
 * <pre><code>   public class Application {
 *       static {
 *           Sabi.uses("foo", new FooDataSrc());
 *           Sabi.uses("bar", new BarDataSrc());
 *       }
 *       public static void main(String ...args) {
 *           int exitCode = 0;
 *           try (var ac = Sabi.setup()) {
 *               ...
 *           } catch (Exception e) {
 *               exitCode = 1;
 *           }
 *           System.exit(exitCode);
 *       }
 *   }</code></pre>
 */
public final class Sabi {
  /**
   * Represents an error reason that occurred when failing to cast the {@code DataHub} instance
   * itself to the expected data access interface type for a {@link Logic}.
   *
   * @param castFromType The actual type of the {@code DataHub} instance that failed to cast.
   */
  public record FailToCastDataHub(String castFromType) {}

  private Sabi() {}

  /**
   * Registers a {@link DataSrc} object with a unique name for global use within the Sabi framework.
   * This method should typically be called in a static initializer block of your application's main
   * class.
   *
   * @param name The unique name to associate with the {@link DataSrc}.
   * @param ds The {@link DataSrc} instance to be registered.
   */
  public static void uses(String name, DataSrc ds) {
    DataHubInner.usesGlobal(name, ds);
  }

  /**
   * Sets up all globally registered {@link DataSrc} objects. This involves calling the {@link
   * DataSrc#setup(AsyncGroup) setup} method on each registered data source. This method should be
   * called once at the application startup.
   *
   * <p>The returned {@link AutoCloseable} object can be used in a try-with-resources statement to
   * automatically invoke the close operations upon exiting the try block.
   *
   * @return An {@link AutoCloseable} object that, when closed, will trigger the global close
   *     operation.
   * @throws Exc if an error occurs during the setup of any {@link DataSrc}.
   */
  public static AutoCloseable setup() throws Exc {
    return DataHubInner.setupGlobals();
  }

  /**
   * Executes the provided application {@link Logic} without transactional boundaries. The {@code
   * DataHub} instance in the parameters is passed as the data access object {@code D} to the {@link
   * Logic}'s {@code run} method.
   *
   * @param <D> The type of the data access object, which typically is {@code DataHub} or an
   *     interface implemented by {@code DataHub} that {@link Logic} expects.
   * @param logic The application logic to execute.
   * @param hub An instance of a DataHub subclass that inherits the data interface for logic
   *     arguments.
   * @throws Exc if an {@link Exc} or {@link RuntimeException} occurs during logic execution or if
   *     the {@code DataHub} cannot be cast to the expected data access type.
   */
  public static <D> void run(Logic<D> logic, DataHub hub) throws Exc {
    D data;
    try {
      @SuppressWarnings("unchecked")
      D d = (D) hub;
      data = d;
    } catch (Exception e) {
      throw new Exc(new FailToCastDataHub(hub.getClass().getName()));
    }
    try {
      hub.begin();
      logic.run(data);
    } catch (Exc | RuntimeException e) {
      throw e;
    } finally {
      hub.end();
    }
  }

  /**
   * Executes the provided application {@link Logic} within a transactional context. The {@code
   * DataHub} instance in the parameter is passed as the data access object {@code D} to the {@link
   * Logic}'s {@code run} method. If the logic completes successfully, a commit operation is
   * attempted. If any {@link Exc}, {@link RuntimeException}, or {@link Error} occurs, a rollback
   * operation is performed.
   *
   * @param <D> The type of the data access object, which typically is {@code DataHub} or an
   *     interface implemented by {@code DataHub} that {@link Logic} expects.
   * @param logic The application logic to execute transactionally.
   * @param hub An instance of a DataHub subclass that inherits the data interface for logic
   *     arguments.
   * @throws Exc if an {@link Exc}, {@link RuntimeException}, or {@link Error} occurs during logic
   *     execution, pre-commit, or commit. The original exception is re-thrown after rollback.
   */
  public static <D> void txn(Logic<D> logic, DataHub hub) throws Exc {
    D data;
    try {
      @SuppressWarnings("unchecked")
      D d = (D) hub;
      data = d;
    } catch (Exception e) {
      throw new Exc(new FailToCastDataHub(hub.getClass().getName()));
    }
    try {
      hub.begin();
      logic.run(data);
      hub.commit();
    } catch (Exc | RuntimeException | Error e) {
      hub.rollback();
      throw e;
    } finally {
      hub.end();
    }
  }
}
