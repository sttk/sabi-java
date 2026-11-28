/*
 * DataConnManager.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import static com.github.sttk.sabi.DataConnContainer.isNoDataConn;

import com.github.sttk.errs.Err;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DataConnManager {
  final List<DataConnContainer> list;
  final Map<String, Integer> indexMap;
  final List<String> orderedNames;
  boolean committed;

  DataConnManager() {
    this.orderedNames = Collections.emptyList();
    this.list = new ArrayList<>();
    this.indexMap = new HashMap<>();
  }

  DataConnManager(List<String> names) {
    this.orderedNames = names.stream().distinct().toList();
    this.list = new ArrayList<>(orderedNames.size());
    this.indexMap = new HashMap<>(orderedNames.size());
    refillContainersByOrderedNames();
  }

  private void refillContainersByOrderedNames() {
    for (int i = 0; i < this.orderedNames.size(); i++) {
      var name = this.orderedNames.get(i);
      this.indexMap.put(name, i);
      this.list.add(new DataConnContainer(name, null));
    }
  }

  <C extends DataConn> C getOrAdd(String name, Class<C> cls, DataConnSupplier<? extends C> supplier)
      throws Err {
    var idx = this.indexMap.get(name);
    if (idx != null) {
      var cont = this.list.get(idx);
      if (!isNoDataConn(cont)) {
        try {
          return cls.cast(cont.conn);
        } catch (ClassCastException e) {
          var fromType = cont.conn.getClass().getName();
          var toType = cls.getName();
          throw new Err(new DataConnError.FailToCastDataConn(name, fromType, toType), e);
        }
      } else {
        C conn = supplier.supply();
        cont = new DataConnContainer(name, conn);
        this.list.set(idx, cont);
        return conn;
      }
    }

    C conn = supplier.supply();
    var cont = new DataConnContainer(name, conn);
    this.indexMap.put(name, this.list.size());
    this.list.add(cont);
    return conn;
  }

  ArrayList<TxnFailureReport.Builder> newTxnFailureReportBuilders(Err logicErr) {
    var lst = new ArrayList<TxnFailureReport.Builder>(this.list.size());
    for (var cont : this.list) {
      if (isNoDataConn(cont)) {
        continue;
      }
      lst.add(new TxnFailureReport.Builder(cont.name, cont.conn.getClass(), logicErr));
    }
    return lst;
  }

  void commit(List<TxnFailureReport.Builder> builders) throws Err {
    Throwable firstThrowable = null;

    var ag = new AsyncGroup();
    int iRep = 0;
    for (var cont : this.list) {
      if (isNoDataConn(cont)) {
        continue;
      }
      ag.name = cont.name;
      ag.index = iRep;
      iRep++;
      try {
        cont.conn.preCommit(ag);
      } catch (Err err) {
        ag.addErr(ag.index, ag.name, err);
        break;
      } catch (RuntimeException e) {
        ag.addErr(ag.index, ag.name, new Err(new AsyncGroupError.RuntimeExceptionOccurred(), e));
        firstThrowable = e;
        break;
      } catch (Throwable t) {
        ag.addErr(ag.index, ag.name, new Err(new AsyncGroupError.ErrorOccurred(), t));
        firstThrowable = t;
        break;
      }
    }
    var errors = ag.join();

    if (!errors.isEmpty()) {
      for (var ee : errors) {
        int idx = ee.index();
        builders.get(idx).cause =
            new TxnFailureCause(TxnFailureCauseState.PreCommitFailure, ee.err());
      }

      if (firstThrowable != null) {
        if (firstThrowable instanceof RuntimeException) {
          throw RuntimeException.class.cast(firstThrowable);
        } else {
          throw Error.class.cast(firstThrowable);
        }
      }
      throw new Err(new DataConnError.FailToPreCommitDataConn(errors));
    }

    ag = new AsyncGroup();
    iRep = 0;
    for (var cont : this.list) {
      if (isNoDataConn(cont)) {
        continue;
      }
      ag.name = cont.name;
      ag.index = iRep;
      iRep++;
      if (!cont.conn.isCommitted()) {
        try {
          cont.conn.commit(ag);
        } catch (Err err) {
          ag.addErr(ag.index, ag.name, err);
          break;
        } catch (RuntimeException e) {
          ag.addErr(ag.index, ag.name, new Err(new AsyncGroupError.RuntimeExceptionOccurred(), e));
          firstThrowable = e;
          break;
        } catch (Throwable t) {
          ag.addErr(ag.index, ag.name, new Err(new AsyncGroupError.ErrorOccurred(), t));
          firstThrowable = t;
          break;
        }
      }
    }
    errors = ag.join();

    if (!errors.isEmpty()) {
      for (var ee : errors) {
        int idx = ee.index();
        builders.get(idx).cause = new TxnFailureCause(TxnFailureCauseState.CommitFailure, ee.err());
      }

      if (firstThrowable != null) {
        if (firstThrowable instanceof RuntimeException) {
          throw RuntimeException.class.cast(firstThrowable);
        } else {
          throw Error.class.cast(firstThrowable);
        }
      }
      throw new Err(new DataConnError.FailToCommitDataConn(errors));
    }

    this.committed = true;

    ag = new AsyncGroup();
    iRep = 0;
    for (var cont : this.list) {
      if (isNoDataConn(cont)) {
        continue;
      }
      ag.name = cont.name;
      ag.index = iRep;
      iRep++;
      try {
        cont.conn.postCommit(ag);
      } catch (Err err) {
        ag.addErr(ag.index, ag.name, err);
        // don't break
      } catch (RuntimeException e) {
        ag.addErr(ag.index, ag.name, new Err(new AsyncGroupError.RuntimeExceptionOccurred(), e));
        if (firstThrowable == null) {
          firstThrowable = e;
        } else {
          firstThrowable.addSuppressed(e);
        }
        // don't break
      } catch (Throwable t) {
        ag.addErr(ag.index, ag.name, new Err(new AsyncGroupError.ErrorOccurred(), t));
        if (firstThrowable == null) {
          firstThrowable = t;
        } else {
          firstThrowable.addSuppressed(t);
        }
        // don't break
      }
    }
    errors = ag.join();

    if (!errors.isEmpty()) {
      for (var ee : errors) {
        int idx = ee.index();
        builders.get(idx).cause =
            new TxnFailureCause(TxnFailureCauseState.PostCommitFailure, ee.err());
      }

      if (firstThrowable != null) {
        if (firstThrowable instanceof RuntimeException) {
          throw RuntimeException.class.cast(firstThrowable);
        } else {
          throw Error.class.cast(firstThrowable);
        }
      }
      throw new Err(new DataConnError.FailToPostCommitDataConn(errors));
    }
  }

  void rollback(List<TxnFailureReport.Builder> builders) {
    Throwable firstThrowable = null;

    var ag = new AsyncGroup();
    int iRep = 0;
    for (var cont : this.list) {
      if (isNoDataConn(cont)) {
        continue;
      }
      ag.name = cont.name;
      ag.index = iRep;
      iRep++;
      if (cont.conn.isCommitted()) {
        if (builders.get(ag.index).cause.state == TxnFailureCauseState.NoneByUncommitted) {
          builders.get(ag.index).cause =
              new TxnFailureCause(TxnFailureCauseState.NoneByCommitted, null);
        }
        continue;
      }
      if (this.committed) {
        continue;
      }
      try {
        cont.conn.rollback(ag);
        builders.get(ag.index).rollback =
            new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      } catch (Err err) {
        ag.addErr(ag.index, ag.name, err);
      } catch (RuntimeException e) {
        ag.addErr(ag.index, ag.name, new Err(new AsyncGroupError.RuntimeExceptionOccurred(), e));
        if (firstThrowable == null) {
          firstThrowable = e;
        } else {
          firstThrowable.addSuppressed(e);
        }
      } catch (Throwable t) {
        ag.addErr(ag.index, ag.name, new Err(new AsyncGroupError.ErrorOccurred(), t));
        if (firstThrowable == null) {
          firstThrowable = t;
        } else {
          firstThrowable.addSuppressed(t);
        }
      }
    }
    var errors = ag.join();

    if (!errors.isEmpty()) {
      for (var ee : errors) {
        int idx = ee.index();
        builders.get(idx).rollback =
            new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, ee.err());
      }
    }

    var reports = builders.stream().map(TxnFailureReport.Builder::build).toList();

    ag = new AsyncGroup();
    for (var cont : this.list) {
      if (!isNoDataConn(cont)) {
        try {
          cont.conn.onTxnFailure(ag, reports);
        } catch (Throwable t) {
          if (firstThrowable == null) {
            firstThrowable = t;
          } else {
            firstThrowable.addSuppressed(t);
          }
        }
      }
    }
    ag.join();

    if (firstThrowable != null) {
      if (firstThrowable instanceof RuntimeException) {
        throw RuntimeException.class.cast(firstThrowable);
      } else {
        throw Error.class.cast(firstThrowable);
      }
    }
  }

  void close() {
    this.indexMap.clear();

    Throwable firstThrowable = null;

    for (int i = this.list.size() - 1; i >= 0; i--) {
      var cont = this.list.get(i);
      if (!isNoDataConn(cont)) {
        try {
          cont.conn.close();
        } catch (Throwable t) {
          if (firstThrowable == null) {
            firstThrowable = t;
          } else {
            firstThrowable.addSuppressed(t);
          }
        }
      }
    }
    this.list.clear();
    refillContainersByOrderedNames();

    this.committed = false;

    if (firstThrowable != null) {
      if (firstThrowable instanceof RuntimeException) {
        throw RuntimeException.class.cast(firstThrowable);
      } else {
        throw Error.class.cast(firstThrowable);
      }
    }
  }

  void abend() {
    for (int i = this.list.size() - 1; i >= 0; i--) {
      var cont = this.list.get(i);
      if (!isNoDataConn(cont)) {
        try {
          cont.conn.abend();
        } catch (Throwable e) {
          // no throw
        }
      }
    }
    this.list.clear();

    this.indexMap.clear();
    this.committed = false;
  }

  @FunctionalInterface
  interface DataConnSupplier<C extends DataConn> {
    C supply() throws Err;
  }
}
