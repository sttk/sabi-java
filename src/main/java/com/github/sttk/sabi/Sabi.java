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
}
