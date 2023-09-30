/*
 * DaxBase class.
 * Copyright (C) 2022-2023 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.github.sttk.sabi.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.async.AsyncGroupAsync;
import com.github.sttk.sabi.async.AsyncGroupSync;

/**
 * {@code DaxBase} is the class that defines the methods to manage
 * {@link DaxSrc}(s).
 * And this class defines private methods to process a transaction.
 */
public class DaxBase implements Dax, AutoCloseable {
  /**
   * {@code FailToSetupGlobalDaxSrcs} is the error reason which indicates that
   * some {@link DaxSrc}(s) failed to set up.
   * 
   * @param errors  The map holding keys that are the registered names of
   *   {@link DaxSrc}(s) failed, and values that are {@link Err}(s) having
   *   their error reasons.
   */
  public record FailToSetupGlobalDaxSrcs(Map<String, Err> errors) {};

  /**
   * {@code FailToSetupLocalDaxSrc} is the error reason which indicates a local
   * {@link DaxSrc} failed to set up.
   *
   * @param name  Tthe registered name of the {@link DaxSrc} failed.
   */
  public record FailToSetupLocalDaxSrc(String name) {};

  /**
   * {@code DaxSrcIsNotFound} is the error reason which indicates that a
   * specified {@link DaxSrc} is not found.
   *
   * @param name  The registered name of the {@link DaxSrc} not found.
   */
  public record DaxSrcIsNotFound(String name) {}

  /**
   * {@code FailToCreateDaxConn} is the error reason which indicates that it is
   * failed to create a new connection to a data store.
   *
   * @param name  The registered name of the {@link DaxSrc} failed to create a
   *   {@link DaxConn}.
   */
  public record FailToCreateDaxConn(String name) {}

  /**
   * {@code FailToCommitDaxConn} is the error reason which indicates that some
   * connections failed to commit.
   *
   * @param errors  The map holding keys that are the names of
   *   {@link DaxConn}(s) failed, and values that are {@link Err}(s) having
   *   their error reasons.
   */
  public record FailToCommitDaxConn(Map<String, Err> errors) {}

  /**
   * {@code CreatedDaxConnIsNull} is the error reason which indicates that a
   * {@link DaxSrc} created a {@link DaxConn} interface but it is null.
   *
   * @param name  The name of the {@link DaxSrc} that try to create a
   *   {@link DaxConn}.
   */
  public record CreatedDaxConnIsNull(String name) {}

  /**
   * {@code FailToRunLogic} is the error reason which indicates that a logic
   * failed to run.
   *
   * @param logicType  The logic class failed.
   */
  public record FailToRunLogic(Class<? extends Logic> logicType) {}

  /** The flag to prevent further registration of global {@link DaxSrc}(s). */
  private static boolean isGlobalDaxSrcsFixed;

  /** The map for registering global {@link DaxSrc} instances. */
  private static final Map<String, DaxSrc> globalDaxSrcMap;

  static {
    globalDaxSrcMap = new LinkedHashMap<>();
  }

  /**
   * Registers to enable data accesses to data store associated with the
   * argument {@link DaxSrc} in all dax instances.
   */
  static void addGlobalDaxSrc(String name, DaxSrc ds) {
    if (isGlobalDaxSrcsFixed) {
      return;
    }
    globalDaxSrcMap.putIfAbsent(name, ds);
  }

  /**
   * Makes all globally registered {@link DaxSrc}(s) usable.
   * This method forbids adding more global {@link DaxSrc}(s), and call each
   * {@link DaxSrc#setup} method of all registered {@link DaxSrc}(s).
   *
   * If one of {@link DaxSrc}(s) fails to execute synchronous {@link
   * DaxSrc#setup}, this method stops other setting up and throws an {@link
   * Err} containing the error reason of that failure.
   *
   * If one of {@link DaxSrc}(s) fails to execute asynchronous {@link
   * DaxSrc#setup}, this function continue to other setting up and throws
   * an {@link Err} containing the error reason of that failure and other
   * errors if any.
   *
   * @throws Err  If an exception occuers by either of the following reasons:
   *  <ul>
   *   <li>{@link com.github.sttk.sabi.DaxBase.FailToSetupGlobalDaxSrcs}
   *     If failing to setup some of global {@link DaxSrc}(s).</li>
   *  </ul>
   */
  static void setupGlobalDaxSrcs() throws Err {
    isGlobalDaxSrcsFixed = true;
    Err.fixCfg();

    var ag = new AsyncGroupAsync<String>();

    for (var ent : globalDaxSrcMap.entrySet()) {
      try {
        ag.name = ent.getKey();
        ent.getValue().setup(ag);
      } catch (Exception e) {
        ag.join();
        ag.addErr(ag.name, e);
        throw new Err(new FailToSetupGlobalDaxSrcs(ag.makeErrs()));
      }
    }

    ag.join();

    if (ag.hasErr()) {
      throw new Err(new FailToSetupGlobalDaxSrcs(ag.makeErrs()));
    }
  }

  /**
   * Closes and frees each resource of registered global {@link DaxSrc}(s).
   * This method should always be called before an application ends.
   */
  static void closeGlobalDaxSrcs() {
    for (var ent : globalDaxSrcMap.entrySet()) {
      ent.getValue().close();
    }
  }

  /** The flag to prevent further registration of local {@link DaxSrc}(s). */
  private boolean isLocalDaxSrcsFixed;

  /** The map for registering local {@link DaxSrc} instances. */
  private Map<String, DaxSrc> localDaxSrcMap = new LinkedHashMap<>();

  /** The map for registering {@link DaxConn} instances. */
  private Map<String, DaxConn> daxConnMap = new LinkedHashMap<>();

  /**
   * The lock to registering {@link DaxConn} exclusively for this
   * {@link DaxBase} instance.
   */
  private final Lock daxConnLock = new ReentrantLock();

  /**
   * The default constructor.
   */
  public DaxBase() {
    isGlobalDaxSrcsFixed = true;
    Err.fixCfg();
  }

  /**
   * Closes and frees all local {@link DaxSrc}(s).
   */
  @Override
  public void close() {
    if (isLocalDaxSrcsFixed) {
      return;
    }

    for (var ds : localDaxSrcMap.values()) {
      ds.close();
    }
    localDaxSrcMap.clear();
  }

  /**
   * Registers and sets up a local {@link DaxSrc} with an argument name.
   *
   * @param name  The name for the {@link DaxSrc} to be registered.
   * @param ds  The {@link DaxSrc} instance to be registered.
   * @throws Err  If an exception occuers by either of the following reasons:
   *  <ul>
   *   <li>{@link com.github.sttk.sabi.DaxBase.FailToSetupLocalDaxSrc}
   *     If failing to setup some of local {@link DaxSrc}(s).</li>
   *  </ul>
   */
  public void uses(String name, DaxSrc ds) throws Err {
    if (isLocalDaxSrcsFixed) {
      return;
    }

    var agSync = new AsyncGroupSync();

    try {
      ds.setup(agSync);
    } catch (Exception e) {
      throw new Err(new FailToSetupLocalDaxSrc(name), e);
    }

    if (agSync.getErr() != null) {
      throw new Err(new FailToSetupLocalDaxSrc(name), agSync.getErr());
    }

    localDaxSrcMap.putIfAbsent(name, ds);
  }

  /**
   * Closes and removes a local {@link DaxSrc} specified by the argument name.
   *
   * @param name  The name of the local {@link DaxSrc} to be removed.
   */
  public void disuses(String name) {
    if (isLocalDaxSrcsFixed) {
      return;
    }

    var ds = localDaxSrcMap.remove(name);
    if (ds != null) {
      ds.close();
    }
  }

  /**
   * Begins a transaction processing.
   * This method forbids registration of more local {@link DaxSrc}(s) while
   * a transaction processing.
   */
  protected void begin() {
    isLocalDaxSrcsFixed = true;
  }

  /**
   * Commits updates in a transaction processing.
   *
   * @throws Err  If an exception occuers by either of the following reasons:
   *  <ul>
   *   <li>{@link com.github.sttk.sabi.DaxBase.FailToCommitDaxConn}
   *     If failing to commit updates via some {@link DaxConn}(s).</li>
   *  </ul>
   */
  protected void commit() throws Err {
    var ag = new AsyncGroupAsync<String>();

    for (var ent : daxConnMap.entrySet()) {
      ag.name = ent.getKey();
      try {
        ent.getValue().commit(ag);
      } catch (Exception e) {
        ag.join();
        ag.addErr(ent.getKey(), e);
        throw new Err(new FailToCommitDaxConn(ag.makeErrs()));
      }
    }

    ag.join();

    if (ag.hasErr()) {
      throw new Err(new FailToCommitDaxConn(ag.makeErrs()));
    }
  }

  /**
   * Rollbacks all updates in a transaction.
   */
  protected void rollback() {
    var ag = new AsyncGroupAsync<String>();

    for (var ent : daxConnMap.entrySet()) {
      var conn = ent.getValue();
      if (conn.isCommitted()) {
        conn.forceBack(ag);
      } else {
        conn.rollback(ag);
      }
    }

    ag.join();
  }

  /**
   * Ends a transaction.
   * This method closes all {@link DaxConn} and removes them from this
   * {@link DaxBase}..
   */
  protected void end() {
    for (var conn : daxConnMap.values()) {
      conn.close();
    }
    daxConnMap.clear();

    isLocalDaxSrcsFixed = false;
  }

  private class ErrWrapper extends RuntimeException {
    Err err;
    ErrWrapper(Err err) {
      this.err = err;
    }
  }

  /**
   * {@inheritDoc}
   */
  public <C extends DaxConn> C getDaxConn(String name) throws Err {
    var conn = daxConnMap.get(name);
    if (conn != null) {
      @SuppressWarnings("unchecked")
      final C c = (C) conn;
      return c;
    }

    daxConnLock.lock();

    try {
      @SuppressWarnings("unchecked")
      final C c = (C) daxConnMap.computeIfAbsent(name, key -> {
        var ds = localDaxSrcMap.get(key);
        if (ds == null) {
          ds = globalDaxSrcMap.get(key);
        }
        if (ds == null) {
          var err = new Err(new DaxSrcIsNotFound(key));
          throw new ErrWrapper(err);
        }

        DaxConn dc = null;
        try {
           dc = ds.createDaxConn();
        } catch (Exception e) {
          var err = new Err(new FailToCreateDaxConn(key), e);
          throw new ErrWrapper(err);
        }
        if (dc == null) {
          var err = new Err(new CreatedDaxConnIsNull(key));
          throw new ErrWrapper(err);
        }
        return dc;
      });

      return c;

    } catch (ErrWrapper e) {
      throw e.err;

    } finally {
      daxConnLock.unlock();
    }
  }

  /**
   * Executes logics in a transaction.
   *
   * First, this method casts the argument {@link DaxBase} to the type
   * specified as a logic's argument.
   * Next, this method begins the transaction, and executes the argument
   * logics.
   * Then, if no error occurs, this method commits all updates in the
   * transaction, otherwise rollbaks them.
   * If there are commit errors after some {@link DaxConn}(s) are committed,
   * or there are {@link DaxConn}(s) which don't have rollback mechanism,
   * this method executes {@link DaxConn#forceBack} methods of these
   * {@link DaxConn}(s).
   * And after that, this method ends the transaction.
   *
   * During a transaction, it is denied to add or remove any local
   * {@link DaxSrc}(s).
   */
  @SafeVarargs
  public final <D> void txn(Logic<D> ...logics) throws Err {
    @SuppressWarnings("unchecked")
    final D dax = (D) this;

    begin();

    try {
      for (var logic : logics) {
        try {
          logic.run(dax);
        } catch (Err e) {
          throw e;
        } catch (Exception e) {
          throw new Err(new FailToRunLogic(logic.getClass()), e);
        }
      }

      commit();

    } catch (Err | RuntimeException | Error e) {
      rollback();
      throw e;

    } finally {
      end();
    }
  }
}
