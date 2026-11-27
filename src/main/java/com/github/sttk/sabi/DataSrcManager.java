/*
 * DataSrcManager.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

class DataSrcManager {

  // Elements can be null, have a null .ds, or have duplicate names.
  final ArrayList<DataSrcContainer> listUnready;

  // Elements cannot be null, cannot have a null .ds, and cannot have duplicate names.
  final ArrayList<DataSrcContainer> listReady;

  final boolean local;
  boolean isAbnormal;

  DataSrcManager(boolean local) {
    this.listUnready = new ArrayList<>(0);
    this.listReady = new ArrayList<>(0);
    this.local = local;
    this.isAbnormal = false;
  }

  void add(String name, DataSrc ds) {
    if (this.isAbnormal) {
      return;
    }
    this.listUnready.add(new DataSrcContainer(this.local, name, ds));
  }

  void remove(String name) {
    final Throwable[] firstThrowable = new Throwable[] {null};

    this.listReady.removeIf(
        cont -> {
          if (Objects.equals(cont.name, name)) {
            try {
              cont.ds.close();
            } catch (Throwable t) {
              if (firstThrowable[0] == null) {
                firstThrowable[0] = t;
              } else {
                firstThrowable[0].addSuppressed(t);
              }
            }
            return true;
          } else {
            return false;
          }
        });

    this.listUnready.removeIf(cont -> (cont == null || Objects.equals(cont.name, name)));

    if (firstThrowable[0] != null) {
      if (firstThrowable[0] instanceof RuntimeException) {
        throw RuntimeException.class.cast(firstThrowable[0]);
      } else {
        throw Error.class.cast(firstThrowable[0]);
      }
    }
  }

  void close() {
    final Throwable[] firstThrowable = new Throwable[1];

    final var closedList = new ArrayList<DataSrcContainer>(this.listReady);
    this.listReady.clear();

    for (var cont : closedList.reversed()) {
      try {
        cont.ds.close();
      } catch (Throwable t) {
        if (firstThrowable[0] == null) {
          firstThrowable[0] = t;
        } else {
          firstThrowable[0].addSuppressed(t);
        }
      }
    }
    this.listUnready.clear();

    if (firstThrowable[0] != null) {
      if (firstThrowable[0] instanceof RuntimeException) {
        throw RuntimeException.class.cast(firstThrowable[0]);
      } else {
        throw Error.class.cast(firstThrowable[0]);
      }
    }
  }

  private void closeOnFatal(List<Integer> indices, Throwable alreadyThrown) {
    this.isAbnormal = true;

    for (Integer i : indices.reversed()) {
      if (i == null) continue;
      var cont = this.listUnready.get(i);
      if (cont == null) continue;
      try {
        cont.ds.close();
      } catch (Throwable t) {
        alreadyThrown.addSuppressed(t);
      }
    }

    for (var cont : this.listReady.reversed()) {
      try {
        cont.ds.close();
      } catch (Throwable t) {
        alreadyThrown.addSuppressed(t);
      }
    }

    this.listUnready.clear();
    this.listReady.clear();
  }

  List<ErrEntry> setup() {
    if (this.listUnready.isEmpty()) {
      return Collections.emptyList();
    }

    Throwable firstThrowable = null;
    var ag = new AsyncGroup();

    var dedupSet = new HashSet<String>();
    for (var cont : this.listReady) {
      dedupSet.add(cont.name);
    }

    try {
      for (int i = 0, n = this.listUnready.size(); i < n; i++) {
        var cont = this.listUnready.get(i);
        if (cont == null) continue;
        if (!dedupSet.add(cont.name)) {
          this.listUnready.set(i, null);
          continue;
        }
        ag.name = cont.name;
        ag.index = i;
        try {
          cont.ds.setup(ag);
        } catch (Err err) {
          ag.addErr(ag.index, ag.name, err);
          break;
        }
      }
    } catch (Throwable t) {
      firstThrowable = t;
    }

    int nDone = ag.index + 1;
    var errors = ag.join();

    if (firstThrowable != null) {
      this.closeOnFatal(IntStream.range(0, nDone).boxed().toList(), firstThrowable);
      if (firstThrowable instanceof RuntimeException) {
        throw RuntimeException.class.cast(firstThrowable);
      } else {
        throw Error.class.cast(firstThrowable);
      }
    }

    if (errors.isEmpty()) {
      this.listReady.addAll(this.listUnready.stream().filter(cont -> cont != null).toList());
      this.listUnready.clear();
      return errors;
    }

    for (int i = nDone - 1; i >= 0; i--) {
      var cont = this.listUnready.get(i);
      if (cont == null) continue;
      try {
        cont.ds.close();
      } catch (Throwable t) {
        if (firstThrowable == null) {
          firstThrowable = t;
        } else {
          firstThrowable.addSuppressed(t);
        }
      }
    }

    if (firstThrowable != null) {
      this.closeOnFatal(Collections.emptyList(), firstThrowable);
      if (firstThrowable instanceof RuntimeException) {
        throw RuntimeException.class.cast(firstThrowable);
      } else {
        throw Error.class.cast(firstThrowable);
      }
    }

    return errors;
  }

  List<ErrEntry> setupWithOrder(List<String> names) {
    if (this.listUnready.isEmpty()) {
      return Collections.emptyList();
    }

    var orderedNames = names.stream().distinct().toList();
    var orderedIndexes = new ArrayList<Integer>(orderedNames.size());
    var indexMap = new HashMap<String, Integer>(orderedNames.size());
    for (int i = 0; i < orderedNames.size(); i++) {
      orderedIndexes.add(null);
      indexMap.put(orderedNames.get(i), i);
    }

    int numUnready = this.listUnready.size();
    orderedIndexes.ensureCapacity(numUnready);

    var dedupSet = new HashSet<String>();
    for (var cont : this.listReady) {
      dedupSet.add(cont.name);
    }

    for (int listIndex = 0; listIndex < numUnready; listIndex++) {
      var cont = this.listUnready.get(listIndex);
      if (cont == null) continue;
      if (!dedupSet.add(cont.name)) {
        this.listUnready.set(listIndex, null);
        continue;
      }
      Integer orderIndex = indexMap.remove(cont.name);
      if (orderIndex != null) {
        orderedIndexes.set(orderIndex, listIndex);
      } else {
        orderedIndexes.add(listIndex);
      }
    }

    int nDone = 0;
    Throwable firstThrowable = null;
    var ag = new AsyncGroup();

    try {
      for (Integer listIndex : orderedIndexes) {
        nDone++;
        if (listIndex == null) {
          continue;
        }
        var cont = this.listUnready.get(listIndex);
        // if (cont == null) continue // impossible
        ag.name = cont.name;
        ag.index = listIndex;
        try {
          cont.ds.setup(ag);
        } catch (Err err) {
          ag.addErr(ag.index, ag.name, err);
          break;
        }
      }
    } catch (Throwable t) {
      firstThrowable = t;
    }

    var errors = ag.join();

    if (firstThrowable != null) {
      this.closeOnFatal(orderedIndexes.subList(0, nDone), firstThrowable);
      if (firstThrowable instanceof RuntimeException) {
        throw RuntimeException.class.cast(firstThrowable);
      } else {
        throw Error.class.cast(firstThrowable);
      }
    }

    if (errors.isEmpty()) {
      this.listReady.ensureCapacity(numUnready);
      for (Integer listIndex : orderedIndexes) {
        if (listIndex == null) {
          continue;
        }
        this.listReady.add(this.listUnready.get(listIndex));
      }
      this.listUnready.clear();
      return errors;
    }

    for (Integer listIndex : orderedIndexes.subList(0, nDone).reversed()) {
      if (listIndex == null) {
        continue;
      }
      var cont = this.listUnready.get(listIndex);
      // if (cont == null) continue // impossible
      try {
        cont.ds.close();
      } catch (Throwable t) {
        if (firstThrowable == null) {
          firstThrowable = t;
        } else {
          firstThrowable.addSuppressed(t);
        }
      }
    }

    if (firstThrowable != null) {
      this.closeOnFatal(Collections.emptyList(), firstThrowable);
      if (firstThrowable instanceof RuntimeException) {
        throw RuntimeException.class.cast(firstThrowable);
      } else {
        throw Error.class.cast(firstThrowable);
      }
    }

    return errors;
  }

  void copyDsReadyToMap(Map<String, DataSrcContainer> contMap) {
    for (var cont : this.listReady) {
      contMap.put(cont.name, cont);
    }
  }

  DataSrcContainer getReadyDs(String name) {
    for (var cont : this.listReady) {
      if (Objects.equals(cont.name, name)) {
        return cont;
      }
    }
    return null;
  }
}
