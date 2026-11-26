/*
 * DataSrcManager.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import static com.github.sttk.sabi.DataSrcContainer.isNoDataSrc;

import com.github.sttk.errs.Err;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

class DataSrcManager {

  final ArrayList<DataSrcContainer> listUnready; // Elements whose .ds is null are possible.
  final ArrayList<DataSrcContainer> listReady; // Elements whose .ds is null are impossible.
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
    final boolean local = this.local;
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

    this.listUnready.removeIf(cont -> (isNoDataSrc(cont) || Objects.equals(cont.name, name)));

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

    final var closedList = new ArrayList<DataSrcContainer>(this.listReady.size());
    this.listReady.removeIf(
        cont -> {
          // if (isNoDataSrc(cont)) continue; // impossible
          closedList.add(cont);
          return true;
        });
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

  private void closeOnFatal(List<Integer> indices) {
    this.isAbnormal = true;

    for (int i : indices.reversed()) {
      var cont = this.listUnready.get(i);
      if (isNoDataSrc(cont)) continue;
      try {
        cont.ds.close();
      } catch (Throwable t) {
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

    try {
      for (int i = 0, n = this.listUnready.size(); i < n; i++) {
        var cont = this.listUnready.get(i);
        if (isNoDataSrc(cont)) continue;
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
      this.closeOnFatal(IntStream.range(0, nDone).boxed().toList());
      if (firstThrowable instanceof RuntimeException) {
        throw RuntimeException.class.cast(firstThrowable);
      } else {
        throw Error.class.cast(firstThrowable);
      }
    }

    if (errors.isEmpty()) {
      this.listReady.addAll(this.listUnready.stream().filter(c -> !isNoDataSrc(c)).toList());
      this.listUnready.clear();
      return errors;
    }

    for (int i = nDone - 1; i >= 0; i--) {
      var cont = this.listUnready.get(i);
      if (isNoDataSrc(cont)) continue;
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
      this.closeOnFatal(Collections.emptyList());
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

    var indexMap = new HashMap<String, Integer>(names.size());
    // Using rev because earlier ones take precedence when names overlap
    for (int n = names.size(), i = n - 1; i >= 0; i--) {
      indexMap.put(names.get(i), i);
    }

    int numUnready = this.listUnready.size();
    var orderedIndexes = new ArrayList<Integer>(Collections.nCopies(names.size(), null));
    orderedIndexes.ensureCapacity(numUnready);

    for (int listIndex = 0; listIndex < numUnready; listIndex++) {
      var cont = this.listUnready.get(listIndex);
      if (isNoDataSrc(cont)) continue;
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
        // if (isNoDataSrc(cont)) continue // impossible
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
      this.closeOnFatal(orderedIndexes.subList(0, nDone));
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
      // if (isNoDataSrc(cont)) continue // impossible
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
      this.closeOnFatal(Collections.emptyList());
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
}
