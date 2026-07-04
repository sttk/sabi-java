/*
 * DataConnManager.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import static com.github.sttk.sabi.DataConn.FailToCommitDataConn;
import static com.github.sttk.sabi.DataConn.FailToPostCommitDataConn;
import static com.github.sttk.sabi.DataConn.FailToPreCommitDataConn;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.TxnFailureCause;
import com.github.sttk.sabi.TxnFailureCauseState;
import com.github.sttk.sabi.TxnFailureRollback;
import com.github.sttk.sabi.TxnFailureRollbackState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataConnManager {
  final ArrayList<DataConnContainer> list;
  final Map<String, Integer> indexMap;
  boolean committed;

  DataConnManager() {
    this.list = new ArrayList<>();
    this.indexMap = new HashMap<>();
  }

  DataConnManager(List<String> names) {
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

  void prepareTxnFailureReportBuilders(ArrayList<TxnFailureReportBuilder> list) {
    list.ensureCapacity(this.indexMap.size());
    for (var cont : this.list) {
      if (cont.conn != null) {
        list.add(new TxnFailureReportBuilder(cont.name, cont.conn.getClass()));
      }
    }
  }

  void commit(List<TxnFailureReportBuilder> builders) throws Err {
    var ag = new AsyncGroupImpl();
    int ii = 0;
    for (var cont : this.list) {
      if (cont.conn == null) {
        continue;
      }
      ag._name = cont.name;
      ag._index = ii;
      ii++;
      try {
        cont.conn.preCommit(ag);
      } catch (Err err) {
        ag.addErr(ag._index, ag._name, err);
        break;
      } catch (RuntimeException re) {
        ag.addErr(ag._index, ag._name, re);
        break;
      }
    }
    var errors = ag.join();

    if (!errors.isEmpty()) {
      for (var ee : errors) {
        int idx = ee.index;
        builders.get(idx).cause = new TxnFailureCause(TxnFailureCauseState.LogicFailure, ee.err);
      }
      throw new Err(new FailToPreCommitDataConn(errors));
    }

    ag = new AsyncGroupImpl();
    ii = 0;
    for (var cont : this.list) {
      if (cont.conn == null) {
        continue;
      }
      ag._name = cont.name;
      ag._index = ii;
      ii++;
      if (!cont.conn.isCommitted()) {
        try {
          cont.conn.commit(ag);
        } catch (Err err) {
          ag.addErr(ag._index, ag._name, err);
          break;
        } catch (RuntimeException re) {
          ag.addErr(ag._index, ag._name, re);
          break;
        }
      }
    }
    errors = ag.join();

    if (!errors.isEmpty()) {
      for (var ee : errors) {
        int idx = ee.index;
        builders.get(idx).cause = new TxnFailureCause(TxnFailureCauseState.CommitFailure, ee.err);
      }
      throw new Err(new FailToCommitDataConn(errors));
    }

    this.committed = true;

    ag = new AsyncGroupImpl();
    ii = 0;
    for (var cont : this.list) {
      if (cont.conn == null) {
        continue;
      }
      ag._name = cont.name;
      ag._index = ii;
      ii++;
      try {
        cont.conn.postCommit(ag);
      } catch (Err err) {
        ag.addErr(ag._index, ag._name, err);
        // don't break
      } catch (RuntimeException re) {
        ag.addErr(ag._index, ag._name, re);
        // don't break
      }
    }
    errors = ag.join();

    if (!errors.isEmpty()) {
      for (var ee : errors) {
        int idx = ee.index;
        builders.get(idx).cause =
            new TxnFailureCause(TxnFailureCauseState.PostCommitFailure, ee.err);
      }
      throw new Err(new FailToPostCommitDataConn(errors));
    }
  }

  void rollback(List<TxnFailureReportBuilder> builders) {
    var ag = new AsyncGroupImpl();
    int ii = 0;
    for (var cont : this.list) {
      if (cont.conn == null) {
        continue;
      }
      ag._name = cont.name;
      ag._index = ii;
      ii++;
      if (cont.conn.isCommitted()) {
        if (builders.get(ag._index).cause.state == TxnFailureCauseState.NoneByUncommitted) {
          builders.get(ag._index).cause =
              new TxnFailureCause(TxnFailureCauseState.NoneByCommitted, null);
        }
        continue;
      }
      if (this.committed) {
        continue;
      }
      try {
        cont.conn.rollback(ag);
        builders.get(ag._index).rollback =
            new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      } catch (Err err) {
        ag.addErr(ag._index, ag._name, err);
      } catch (RuntimeException re) {
        ag.addErr(ag._index, ag._name, re);
      }
    }
    var errors = ag.join();

    if (!errors.isEmpty()) {
      for (var ee : errors) {
        int idx = ee.index;
        builders.get(idx).rollback =
            new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, ee.err);
      }
    }

    ag = new AsyncGroupImpl();
    for (var cont : this.list) {
      if (cont.conn != null) {
        var reports = builders.stream().map(TxnFailureReportBuilder::build).toList();
        cont.conn.onTxnFailure(ag, reports);
      }
    }
    ag.join();
  }

  void close() {
    this.indexMap.clear();

    for (int i = this.list.size() - 1; i >= 0; i--) {
      var cont = this.list.get(i);
      if (cont.conn != null) {
        cont.conn.close();
      }
    }
    this.list.clear();
  }
}
