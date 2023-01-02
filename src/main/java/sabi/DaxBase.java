/*
 * DaxBase class.
 * Copyright (C) 2022 Takayuki Sato. All Rights Reserved.
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
   * Makes unable to register any further global {@link DaxSrc}.
   */
  public static synchronized void fixGlobalDaxSrcs() {
    isGlobalDaxSrcsFixed = true;
  }

  /**
   * The default constructor of this class.
   */
  public DaxBase() {}

  /**
   * Registers a local {@link DaxSrc} with a specified name.
   *
   * @param name  The name for the argument {@link DaxSrc} and also for a 
   *   {@link DaxConn} created by the argument {@link DaxSrc}.
   *   This name is used to get a {@link DaxConn} with {@link #getDaxConn}
   *   method.
   * @param ds  A {@link DaxSrc} object to be registered locally to enable to
   *   be used in only specific transactions.
   */
  public void addLocalDaxSrc(final String name, final DaxSrc ds) {
    if (!isLocalDaxSrcsFixed) {
      localDaxSrcMap.put(name, ds);
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
  public DaxConn getDaxConn(final String name) throws Err {
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

  void close() {
    for (var conn : this.daxConnMap.values()) {
      try {
        conn.close();
      } catch (Throwable t) {}
    }

    this.isLocalDaxSrcsFixed = false;
  }
}
