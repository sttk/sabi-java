/*
 * DataSrcManager.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.DataSrc;
import com.github.sttk.sabi.ErrEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DataSrcManager {
  final boolean local;
  final List<DataSrcContainer> listUnready;
  final List<DataSrcContainer> listReady;

  DataSrcManager(boolean local) {
    this.local = local;
    this.listUnready = new ArrayList<>(0);
    this.listReady = new ArrayList<>(0);
  }

  void add(String name, DataSrc ds) {
    this.listUnready.add(new DataSrcContainer(this.local, name, ds));
  }

  void remove(String name) {
    for (var cont : this.listReady) {
      if (Objects.equals(cont.name, name) && cont.ds != null) {
        cont.ds.close();
        cont.ds = null;
      }
    }
    for (var cont : this.listUnready) {
      if (Objects.equals(cont.name, name) && cont.ds != null) {
        cont.ds = null;
      }
    }
  }

  void close() {
    for (int i = this.listReady.size() - 1; i >= 0; i--) {
      var cont = this.listReady.get(i);
      if (cont.ds != null) {
        cont.ds.close();
        cont.ds = null;
      }
    }
    for (int i = this.listUnready.size() - 1; i >= 0; i--) {
      var cont = this.listUnready.get(i);
      if (cont.ds != null) {
        cont.ds = null;
      }
    }
    this.listReady.clear();
    this.listUnready.clear();
  }

  List<ErrEntry> setup() {
    if (this.listUnready.isEmpty()) {
      return Collections.emptyList();
    }

    var ag = new AsyncGroupImpl();
    int ii = 0, nDone = 0;
    for (int i = 0, n = this.listUnready.size(); i < n; i++) {
      var cont = this.listUnready.get(i);
      if (cont == null || cont.ds == null) {
        continue;
      }
      ag._name = cont.name;
      ag._index = ii;
      ii++;
      try {
        cont.ds.setup(ag);
      } catch (Err err) {
        ag.addErr(ag._index, ag._name, err);
        nDone = i;
        break;
      } catch (RuntimeException re) {
        ag.addErr(ag._index, ag._name, re);
        nDone = i;
        break;
      }
    }
    var errors = ag.join();

    if (errors.isEmpty()) {
      for (int i = 0, n = this.listUnready.size(); i < n; i++) {
        var cont = this.listUnready.get(i);
        if (cont.ds != null) {
          this.listReady.add(cont);
        }
      }
      this.listUnready.clear();
      return Collections.emptyList();
    } else {
      for (int i = nDone - 1; i >= 0; i--) {
        var cont = this.listUnready.get(i);
        if (cont.ds != null) {
          cont.ds.close();
        }
      }
      return errors;
    }
  }

  List<ErrEntry> setupWithOrder(List<String> names) {
    if (this.listUnready.isEmpty()) {
      return Collections.emptyList();
    }

    var indexedMap = new HashMap<String, Integer>(names.size());
    // Becuase earlier ones take precedence when names overlap
    for (int i = names.size() - 1; i >= 0; i--) {
      indexedMap.put(names.get(i), i);
    }

    var orderedIndexes = new ArrayList<Integer>(this.listUnready.size());
    for (int i = 0, n = names.size(); i < n; i++) {
      orderedIndexes.add(null); // null indicates unset
    }

    for (int listIndex = 0, n = this.listUnready.size(); listIndex < n; listIndex++) {
      var cont = this.listUnready.get(listIndex);
      if (cont != null && cont.ds != null) {
        Integer orderIndex = indexedMap.get(cont.name);
        if (orderIndex != null) {
          orderedIndexes.set(orderIndex, listIndex);
          indexedMap.remove(cont.name);
        } else {
          orderedIndexes.add(listIndex);
        }
      }
    }

    var ag = new AsyncGroupImpl();
    int ii = 0, nDone = 0;
    for (int orderIndex = 0, n = orderedIndexes.size(); orderIndex < n; orderIndex++) {
      Integer listIndex = orderedIndexes.get(orderIndex);
      if (listIndex == null) { // ignore unset
        continue;
      }
      var cont = this.listUnready.get(listIndex);
      if (cont == null || cont.ds == null) {
        continue;
      }
      ag._name = cont.name;
      ag._index = ii;
      ii++;
      try {
        cont.ds.setup(ag);
      } catch (Err err) {
        ag.addErr(ag._index, ag._name, err);
        nDone = orderIndex;
        break;
      } catch (RuntimeException re) {
        ag.addErr(ag._index, ag._name, re);
        nDone = orderIndex;
        break;
      }
    }
    var errors = ag.join();

    if (errors.isEmpty()) {
      for (Integer listIndex : orderedIndexes) {
        if (listIndex == null) { // ignore unset
          continue;
        }
        var cont = this.listUnready.get(listIndex);
        if (cont == null || cont.ds == null) {
          continue;
        }
        this.listReady.add(cont);
      }
      this.listUnready.clear();
      return Collections.emptyList();
    } else {
      for (int orderIndex = nDone - 1; orderIndex >= 0; orderIndex--) {
        Integer listIndex = orderedIndexes.get(orderIndex);
        if (listIndex != null) { // Ignore unset
          var cont = this.listUnready.get(listIndex);
          if (cont.ds != null) {
            cont.ds.close();
          }
        }
      }
      return errors;
    }
  }

  void copyDsReadyToMap(Map<String, DataSrcContainer> contMap) {
    for (var cont : this.listReady) {
      contMap.put(cont.name, cont);
    }
  }
}
