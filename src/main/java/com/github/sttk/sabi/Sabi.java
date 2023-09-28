/*
 * Sabi class.
 * Copyright (C) 2022-2023 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.sabi.errs.Err;

/**
 * {@code Sabi} is the class that provies the static methods related to the
 * global fanctionalities of sabi framework.
 * <p>
 * This class declares {@link #uses uses} method to register a {@link DaxSrc}
 * object used globally with its name.
 * And this class also declares {@link #setup setup}, {@link #close close},
 * and {@link #startApp startApp} methods.
 * {@link #setup setup} is the static method to setup all global registered
 * {@link DaxSrc} objects, and {@link #close close} is the static method to
 * free each resource of global {@link DaxSrc} objects.
 * {@link #startApp startApp} is the method that executes {@link #setup setup}
 * and return {@link AutoCloseable} object which executes the {@link #close
 * close} method in its {@link AutoCloseable#close} method.
 * <p>
 * The usages of these static methods is as follows:
 *
 * <pre><code>   public class Application {
 *       static {
 *           Sabi.uses("foo", new FooDaxSrc());
 *           Sabi.uses("bar", new BarDaxSrc());
 *       }
 *       public static void main(String ...args) {
 *           int exitCode = 0;
 *           try {
 *               Sabi.setup();
 *               ...
 *           } catch (Exception e) {
 *               exitCode = 1;
 *           } finally {
 *               Sabi.close();
 *           }
 *           System.exit(exitCode);
 *       }
 *   }</code></pre>
 * 
 * Or,
 * 
 * <pre><code>   public class Application {
 *       static {
 *           Sabi.uses("foo", new FooDaxSrc());
 *           Sabi.uses("bar", new BarDaxSrc());
 *       }
 *       public static void main(String ...args) {
 *           try (var ac = Sabi.startApp()) {
 *               ...
 *           } catch (Exception e) {
 *               System.exit(1);
 *           }
 *       }
 *   }</code></pre>
 */
public final class Sabi {

  /**
   * The default constructor.
   */
  private Sabi() {}

  /**
   * Registers a global {@link DaxSrc} object with its name to enable to use
   * {@link DaxConn} created by the argument {@link DaxSrc} in all
   * transactions.
   * <p>
   * If a {@link DaxSrc} is tried to register with a name already registered,
   * it is ignored and a {@link DaxSrc} registered with the same name first is
   * used.
   * And this method ignore adding {@link DaxSrc}(s) after {@link #setup setup}
   * or beginning of {@link DaxBase#txn}.
   *
   * @param name  The name for the argument {@link DaxSrc} object.
   * @param ds  A {@link DaxSrc} object.
   */
  public static void uses(String name, DaxSrc ds) {
    DaxBase.addGlobalDaxSrc(name, ds);
  }

  /**
   * Makes the globally registered {@link DaxSrc} object usable.
   * <p>
   * This method forbids adding more global {@link DaxSrc}, and called each
   * {@link DaxSrc#setup setup} method of all registered {@link DaxSrc}
   * objects.
   * If one of global {@link DaxSrc} objects fails to execute synchronous
   * {@link DaxSrc#setup setup}, this function stops other setting up and
   * returns an {@link Err} containing the error reason of that failure.
   * <p>
   * If one of global {@link DaxSrc} objects fails to execute asynchronous
   * {@link DaxSrc#setup setup}, this function continue to other setting up
   * and returns an {@link Err} containing the error reason of that failure
   * and other errors if any.
   *
   * @throws Err  If one of global {@link DaxSrc} objects.
   */
  public static void setup() throws Err {
    DaxBase.setupGlobalDaxSrcs();
  }

  /**
   * Closes and frees each resource of registered global {@link DaxSrc}
   * objects.
   */
  public static void close() {
    DaxBase.closeGlobalDaxSrcs();
  }

  /**
   * Executes {@link #setup setup} method, and returns an {@link AutoCloseable}
   * object of which {@link AutoCloseable#close close} method executes {@link
   * #close close} method of this class in it.
   *
   * @return  An {@link AutoCloseable} object.
   * @throws Err  If one of global {@link DaxSrc} objects.
   */
  public static AutoCloseable startApp() throws Err {
    try {
      DaxBase.setupGlobalDaxSrcs();
    } catch (Err | RuntimeException | Error e) {
      DaxBase.closeGlobalDaxSrcs();
      throw e;
    }

    return new Closer();
  }
}

/**
 * Closer is the private class.
 */
class Closer implements AutoCloseable {
  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    DaxBase.closeGlobalDaxSrcs();
  }
}
