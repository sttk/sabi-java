/*
 * DaxBase class.
 * Copyright (C) 2022-2023 Takayuki Sato. All Rights Reserved.
 */
package sabi;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * DaxBase manages multiple {@link DaxSrc} and those {@link DaxConn}, and also
 * works as an implementation of 'Dax' interface.
 */
public abstract class DaxBase {

  /**
   * An error reason which indicates that some dax sources failed to start up.
   * 
   * @param errors  A map of which keys are the registered names of {@link
   * DaxSrc}(s) which failed to start up, and of which values are {@link Err}
   * having their error reasons.
   *
   * @param errors  A map of {@link DaxSrc} names and {@link Err}(s).
   */
  public record FailToStartUpGlobalDaxSrcs(Map<String, Err> errors) {}

  /**
   * An error reason which indicates that a specified {@link DaxSrc} instance
   * is not found.
   *
   * @param name  A registered name of a {@link DaxSrc} is not found.
   */
  public record DaxSrcIsNotFound(String name) {};

  /**
   * An error reason which indicates that it failed to create a new connection
   * to a data source.
   *
   * @param name  A registered name of a {@link DaxSrc} which failed to create *   a {@link DaxConn}.
   */
  public record FailToCreateDaxConn(String name) {};

  /**
   * An error reason which indicates that it is failed to cast type of a
   * DaxConn.
   * The field: name is a registered name of a DaxSrc which created the target
   * DaxConn.
   * And the fields: fromType and toType are the types of the source DaxConn
   * and the destination DaxConn.
   */
  public record FailToCastDaxConn(String name, String fromType, String toType) {};

  /**
   * An error reason which indicates that some connections failed to commit.
   * 
   * @param errors  A map of which keys are registered names of {@link DaxConn}
   *   which failed to commit, and of which values are {@link Err} thrown by
   *   {@link DaxConn#commit} methods.
   */
  public record FailToCommitDaxConn(Map<String, Err> errors) {}

  /**
   * An error reason which indicates that a {@link DaxConn} of the specified
   * name caused an exception.
   *
   * @param name  A name of {@link DaxConn} which caused an exception.
   */
  public record CommitExceptionOccurs(String name) {}

  /** The global flag which fixes global {@link DaxSrc} compositions. */
  private static boolean isGlobalDaxSrcsFixed = false;

  /** The global map which composes global {@link DaxSrc} objects. */
  private static final Map<String, DaxSrc> globalDaxSrcMap = new LinkedHashMap<>();

  /** The local flag which fixes local {@link DaxSrc} compositions. */
  private boolean isLocalDaxSrcsFixed = false;

  /** The local map which composes local {@link DaxSrc} objects. */ 
  private final Map<String, DaxSrc> localDaxSrcMap = new LinkedHashMap<>();

  /** The local map which composes {@link DaxConn} objects. */
  private final Map<String, DaxConn> daxConnMap = new LinkedHashMap<>();

  /**
   * Registers a global {@link DaxSrc} with its name to make enable to use
   * {@link DaxSrc} in all transactions.
   *
   * @param name  The name for the argument {@link DaxSrc} and also for a 
   *   {@link DaxConn} created by the argument {@link DaxSrc}.
   *   This name is used to get a {@link DaxConn} with {@link #getDaxConn}
   *   method.
   * @param ds  A {@link DaxSrc} object to be registered globally to enable to
   *   be used in all transactions.
   */
  public static synchronized void addGlobalDaxSrc(final String name, final DaxSrc ds) {
    if (!isGlobalDaxSrcsFixed) {
      globalDaxSrcMap.put(name, ds);
    }
  }

  /**
   * Forbids adding global dax sources and makes available the registered
   * global dax sources by calling {@link DaxSrc#setUp} method.
   * If even one {@link DaxSrc} fail to execute its {@link DaxSrc#setUp}
   * method, this function executes Free methods of all global {@link
   * DaxSrc}(s) and throws an {@link Err} object.
   *
   * @throws Err  If even one {@link DaxSrc} failed to execute {@link
   *   DaxSrc#setUp} method.
   */
  public static synchronized void startUpGlobalDaxSrcs() throws Err {
    isGlobalDaxSrcsFixed = true;

    var errors = new HashMap<String, Err>();

    for (var entry : globalDaxSrcMap.entrySet()) {
      var name = entry.getKey();
      var ds = entry.getValue();
      try {
        ds.setUp();
      } catch (Err err) {
        errors.put(name, err);
      }
    }

    if (!errors.isEmpty()) {
      shutdownGlobalDaxSrcs();
      throw new Err(new FailToStartUpGlobalDaxSrcs(errors));
    }
  }

  /**
   * Terminates all global dax sources and frees resources of all global dax
   * sources.
   */
  public static synchronized void shutdownGlobalDaxSrcs() {
    for (var ds : globalDaxSrcMap.values()) {
      ds.end();
    }
  }

  /**
   * The default constructor of this class.
   */
  public DaxBase() {}

  /**
   * Registers a local {@link DaxSrc} with a specified name and sets up it.
   *
   * @param name  The name for the argument {@link DaxSrc} and also for a 
   *   {@link DaxConn} created by the argument {@link DaxSrc}.
   *   This name is used to get a {@link DaxConn} with {@link #getDaxConn}
   *   method.
   * @param ds  A {@link DaxSrc} object to be registered locally to enable to
   *   be used in only specific transactions.
   * @throws Err  If the specified {@link DaxSrc} failed to execute {@link
   *   DaxSrc#setUp} method.
   */
  public synchronized void setUpLocalDaxSrc(final String name, final DaxSrc ds) throws Err {
    if (!this.isLocalDaxSrcsFixed) {
      try {
        ds.setUp();
      } catch (Err err) {
        throw err;
      }
      this.localDaxSrcMap.put(name, ds);
    }
  }

  /**
   * Removes a local {@link DaxSrc} of the specified name from this {@link
   * DaxBase} and frees the resource of it.
   *
   * @param name  A {@link DaxSrc} name.
   */
  public synchronized void freeLocalDaxSrc(final String name) {
    if (!this.isLocalDaxSrcsFixed) {
      var ds = this.localDaxSrcMap.remove(name);
      if (ds != null) {
        ds.end();
      }
    }
  }

  /**
   * Removes all local {@link DaxSrc}(s) from this {@link DaxBase} and frees
   * the resources of them.
   */
  public synchronized void freeAllLocalDaxSrcs() {
    if (!this.isLocalDaxSrcsFixed) {
      for (var ds : this.localDaxSrcMap.values()) {
        ds.end();
      }
      this.localDaxSrcMap.clear();
    }
  }

  /**
   * Gets a {@link DaxConn} which is a connection to a data source by specified
   * name.
   * If a {@link DaxConn} is found, this method returns it, but not found,
   * this method creates a new one with a local or global {@link DaxSrc} with
   * same name.
   * If there are both local and global {@link DaxSrc} with same name, the
   * local {@link DaxSrc} is used.
   *
   * @param name  The name of {@link DaxConn} or {@link DaxSrc}.
   * @return  A {@link DaxConn} object.
   * @throws Err  If the following error occured:
   *   <ul>
   *    <li>{@link sabi.DaxBase.DaxSrcIsNotFound} -
   *      If {@link DaxSrc} with the specified name is not found.</li>
   *   </ul>
   */
  public <C extends DaxConn> C getDaxConn(final String name, final Class<C> cls) throws Err {
    var conn = _getDaxConn(name);
    try {
      return cls.cast(conn);
    } catch (Exception e) {
      var from = conn != null ? conn.getClass().getName() : null;
      var to = cls.getName();
      throw new Err(new FailToCastDaxConn(name, from, to), e);
    }
  }

  private DaxConn _getDaxConn(final String name) throws Err {
    var conn = this.daxConnMap.get(name);
    if (conn != null) {
      return conn;
    }

    var ds = this.localDaxSrcMap.get(name);
    if (ds == null) {
      ds = globalDaxSrcMap.get(name);
    }
    if (ds == null) {
      throw new Err(new DaxSrcIsNotFound(name));
    }

    synchronized (this.daxConnMap) {
      conn = this.daxConnMap.get(name);
      if (conn != null) {
        return conn;
      }

      try {
        conn = ds.createDaxConn();
      } catch (Err e) {
        throw new Err(new FailToCreateDaxConn(name), e);
      }

      this.daxConnMap.put(name, conn);
    }

    return conn;
  }

  void begin() {
    this.isLocalDaxSrcsFixed = true;
    isGlobalDaxSrcsFixed = true;
  }

  void commit() throws Err {
    var errors = new HashMap<String, Err>();

    for (var entry : this.daxConnMap.entrySet()) {
      try {
        var conn = entry.getValue();
        conn.commit();
      } catch (Err err) {
        errors.put(entry.getKey(), err);
        break;
      } catch (Exception exc) {
        var err = new Err(new CommitExceptionOccurs(entry.getKey()), exc);
        errors.put(entry.getKey(), err);
        break;
      }
    }

    if (!errors.isEmpty()) {
      throw new Err(new FailToCommitDaxConn(errors));
    }
  }

  void rollback() {
    for (var conn : this.daxConnMap.values()) {
      try {
        conn.rollback();
      } catch (Throwable t) {}
    }
  }

  void end() {
    for (var conn : this.daxConnMap.values()) {
      try {
        conn.close();
      } catch (Throwable t) {}
    }

    this.daxConnMap.clear();

    this.isLocalDaxSrcsFixed = false;
  }
}
