/*
 * DataSrcContainer class.
 * Copyright (C) 2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.DataSrc;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DataSrcList {
  DataSrcContainer notSetupHead;
  DataSrcContainer notSetupLast;
  DataSrcContainer didSetupHead;
  DataSrcContainer didSetupLast;
  boolean local;

  DataSrcList(boolean local) {
    this.local = local;
  } 

  void appendContainerPtrNotSetup(DataSrcContainer ptr) {
    ptr.next = null;

    if (this.notSetupLast == null) {
      this.notSetupHead = ptr;
      this.notSetupLast = ptr;
      ptr.prev = null;
    } else {
      this.notSetupLast.next = ptr;
      ptr.prev = this.notSetupLast;
      this.notSetupLast = ptr;
    }
  }

  void removeContainerPtrNotSetup(DataSrcContainer ptr) {
    var prev = ptr.prev;
    var next = ptr.next;

    if (prev == null && next == null) {
      this.notSetupHead = null;
      this.notSetupLast = null;
    } else if (prev == null) {
      next.prev = null;
      this.notSetupHead = next;
    } else if (next == null) {
      prev.next = null;
      this.notSetupLast = prev;
    } else {
      next.prev = prev;
      prev.next = next;
    }
  }

  void removeAndCloseContainerPtrNotSetupByName(String name) {
    var ptr = this.notSetupHead;
    while (ptr != null) {
      if (Objects.equals(ptr.name, name)) {
        this.removeContainerPtrNotSetup(ptr);
        ptr.ds.close();
      }
      ptr = ptr.next;
    }
  }

  void appendContainerPtrDidSetup(DataSrcContainer ptr) {
    ptr.next = null;

    if (this.didSetupLast == null) {
      this.didSetupHead = ptr;
      this.didSetupLast = ptr;
      ptr.prev = null;
    } else {
      this.didSetupLast.next = ptr;
      ptr.prev = this.didSetupLast;
      this.didSetupLast = ptr;
    }
  }

  void removeContainerPtrDidSetup(DataSrcContainer ptr) {
    var prev = ptr.prev;
    var next = ptr.next;

    if (prev == null && next == null) {
      this.didSetupHead = null;
      this.didSetupLast = null;
    } else if (prev == null) {
      next.prev = null;
      this.didSetupHead = next;
    } else if (next == null) {
      prev.next = null;
      this.didSetupLast = prev;
    } else {
      next.prev = prev;
      prev.next = next;
    }
  }

  void removeAndCloseContainerPtrDidSetupByName(String name) {
    var ptr = this.didSetupHead;
    while (ptr != null) {
      if (Objects.equals(ptr.name, name)) {
        removeContainerPtrDidSetup(ptr);
        ptr.ds.close();
      }
      ptr = ptr.next;
    }
  }

  void copyContainerPtrsDidSetupInto(Map<String, DataSrcContainer> m) {
    var ptr = this.didSetupHead;
    while (ptr != null) {
      m.put(ptr.name, ptr);
      ptr = ptr.next;
    }
  }

  void addDataSrc(String name, DataSrc ds) {
    var ptr = new DataSrcContainer(this.local, name, ds);
    this.appendContainerPtrNotSetup(ptr);
  }

  Map<String, Exc> setupDataSrcs() {
    var excMap = new HashMap<String, Exc>();

    if (this.notSetupHead == null) {
      return excMap;
    }

    var ag = new AsyncGroupImpl();

    var ptr = this.notSetupHead;
    while (ptr != null) {
      ag.name = ptr.name;
      try {
        ptr.ds.setup(ag);
      } catch (Exc exc) {
        excMap.put(ptr.name, exc);
        break;
      }
      ptr = ptr.next;
    }

    ag.joinAndPutExcsInto(excMap);

    var firstPtrNotSetupYet = ptr;

    ptr = this.notSetupHead;
    while (ptr != null && ptr != firstPtrNotSetupYet) {
      var next = ptr.next;
      if (! excMap.containsKey(ptr.name)) {
        this.removeContainerPtrNotSetup(ptr);
        this.appendContainerPtrDidSetup(ptr);
      }
      ptr = next;
    }

    return excMap;
  }

  void closeDataSrcs() {
    var ptr = this.didSetupLast;
    while (ptr != null) {
      ptr.ds.close();
      ptr = ptr.prev;
    }
    this.notSetupHead = null;
    this.notSetupLast = null;
    this.didSetupHead = null;
    this.didSetupLast = null;
  }
}
