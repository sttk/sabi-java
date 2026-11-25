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
  final ArrayList<DataConnContainer> list;
  final Map<String, Integer> indexMap;
  final List<String> orderedNames;
  boolean committed;

  DataConnManager() {
    this.orderedNames = Collections.emptyList();
    this.list = new ArrayList<>();
    this.indexMap = new HashMap<>();
  }

  DataConnManager(List<String> names) {
    this.orderedNames = Collections.unmodifiableList(names);
    this.list = new ArrayList<>(names.size());
    this.indexMap = new HashMap<>(names.size());

    // Because earlier ones take precedence when names overlap
    for (int i = names.size() - 1; i >= 0; i--) {
      this.indexMap.put(names.get(i), i);
      this.list.add(new DataConnContainer(names.get(i), null));
    }
  }

  void add(DataConnContainer cont) {
    var idx = this.indexMap.get(cont.name);
    if (idx != null) {
      // Because earlier ones take precedence when names overlap
      if (this.list.get(idx).conn == null) {
        this.list.set(idx, cont);
      }
    } else {
      this.indexMap.put(cont.name, this.list.size());
      this.list.add(cont);
    }
  }

  <C extends DataConn> C findByNameAndCast(String name, Class<C> cls) throws Err {
    var index = this.indexMap.get(name);
    if (index == null) {
      return null;
    }
    var cont = this.list.get(index);
    if (isNoDataConn(cont)) {
      return null;
    }

    try {
      return cls.cast(cont.conn);
    } catch (Exception e) {
      var fromType = cont.conn.getClass().getName();
      var toType = cls.getName();
      throw new Err(new DataConnError.FailToCastDataConn(name, fromType, toType), e);
    }
  }

  ArrayList<TxnFailureReport.Builder> newTxnFailureReportBuilders(Err logicErr) {
    var list = new ArrayList<TxnFailureReport.Builder>(this.list.size());
    for (var cont : this.list) {
      if (cont.conn == null) {
        continue;
      }
      list.add(new TxnFailureReport.Builder(cont.name, cont.conn.getClass(), logicErr));
    }
    return list;
  }

  void commit(List<TxnFailureReport.Builder> builders) throws Err {
    Throwable firstThrowable = null;

    var ag = new AsyncGroup();
    int iRep = 0;
    for (var cont : this.list) {
      if (cont.conn == null) {
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
      if (cont.conn == null) {
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
      if (cont.conn == null) {
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
        }
        // don't break
      } catch (Throwable t) {
        ag.addErr(ag.index, ag.name, new Err(new AsyncGroupError.ErrorOccurred(), t));
        if (firstThrowable == null) {
          firstThrowable = t;
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
      if (cont.conn == null) {
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
        }
      } catch (Throwable t) {
        ag.addErr(ag.index, ag.name, new Err(new AsyncGroupError.ErrorOccurred(), t));
        if (firstThrowable == null) {
          firstThrowable = t;
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
      if (cont.conn != null) {
        try {
          cont.conn.onTxnFailure(ag, reports);
        } catch (Throwable t) {
          if (firstThrowable == null) {
            firstThrowable = t;
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
      if (cont.conn != null) {
        try {
          cont.conn.close();
        } catch (Throwable t) {
          if (firstThrowable == null) {
            firstThrowable = t;
          }
        }
      }
    }
    this.list.clear();

    for (int i = 0, n = this.orderedNames.size(); i < n; i++) {
      var name = this.orderedNames.get(i);
      this.indexMap.put(name, i);
      this.list.add(new DataConnContainer(name, null));
    }

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
      if (cont.conn != null) {
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
}
