package com.github.sttk.sabi.internal;

import static com.github.sttk.sabi.internal.TestCommonsTest.AsyncDataSrc;
import static com.github.sttk.sabi.internal.TestCommonsTest.Fail;
import static com.github.sttk.sabi.internal.TestCommonsTest.SyncDataSrc;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DataSrcManagerTest {
  private DataSrcManagerTest() {}

  @Test
  void testNew() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    assertThat(manager.local).isTrue();
    assertThat(manager.listUnready).isEmpty();
    assertThat(manager.listReady).isEmpty();

    manager = new DataSrcManager(false);
    assertThat(manager.local).isFalse();
    assertThat(manager.listUnready).isEmpty();
    assertThat(manager.listReady).isEmpty();
  }

  @Test
  void testAdd() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("foo", ds1);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(1);
      assertThat(manager.listReady).hasSize(0);

      assertThat(manager.listUnready.get(0).name).isEqualTo("foo");

      var ds2 = new AsyncDataSrc(2, logger, Fail.None);
      manager.add("bar", ds2);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(2);
      assertThat(manager.listReady).hasSize(0);

      assertThat(manager.listUnready.get(0).name).isEqualTo("foo");
      assertThat(manager.listUnready.get(1).name).isEqualTo("bar");
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(2);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.next()).isEqualTo("AsyncDataSrc#new 2");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testRemove() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("foo", ds1);

      var ds2 = new AsyncDataSrc(2, logger, Fail.None);
      manager.add("bar", ds2);

      var errors = manager.setup();
      assertThat(errors).hasSize(0);

      var ds3 = new SyncDataSrc(3, logger, Fail.None);
      manager.add("baz", ds3);

      var ds4 = new AsyncDataSrc(4, logger, Fail.None);
      manager.add("qux", ds4);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(2);
      assertThat(manager.listReady).hasSize(2);

      manager.remove("baz");
      manager.remove("foo");
      manager.remove("qux");
      manager.remove("bar");

    } finally {
      // manager.close(); // to see Close logs by remove
    }

    assertThat(logger).hasSize(8);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.next()).isEqualTo("AsyncDataSrc#new 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
    assertThat(iter.next()).isEqualTo("AsyncDataSrc#setup 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
    assertThat(iter.next()).isEqualTo("AsyncDataSrc#new 4");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
    assertThat(iter.next()).isEqualTo("AsyncDataSrc#close 2");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testClose() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("foo", ds1);

      var ds2 = new AsyncDataSrc(2, logger, Fail.None);
      manager.add("bar", ds2);

      var errors = manager.setup();
      assertThat(errors).hasSize(0);

      var ds3 = new SyncDataSrc(3, logger, Fail.None);
      manager.add("baz", ds3);

      var ds4 = new AsyncDataSrc(4, logger, Fail.None);
      manager.add("qux", ds4);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(2);
      assertThat(manager.listReady).hasSize(2);
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(8);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.next()).isEqualTo("AsyncDataSrc#new 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
    assertThat(iter.next()).isEqualTo("AsyncDataSrc#setup 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
    assertThat(iter.next()).isEqualTo("AsyncDataSrc#new 4");
    assertThat(iter.next()).isEqualTo("AsyncDataSrc#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void setupNoDataSrc() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setup();
      assertThat(errors).isEmpty();

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);
    } finally {
      manager.close();
    }

    assertThat(logger).isEmpty();
    var iter = logger.iterator();
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void setupAndOk() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("foo", ds1);

      var ds2 = new AsyncDataSrc(2, logger, Fail.None);
      manager.add("bar", ds2);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(2);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setup();
      assertThat(errors).hasSize(0);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(2);
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(6);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.next()).isEqualTo("AsyncDataSrc#new 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
    assertThat(iter.next()).isEqualTo("AsyncDataSrc#setup 2");
    assertThat(iter.next()).isEqualTo("AsyncDataSrc#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void setupButError() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.Setup);
      manager.add("bar", ds2);

      var ds3 = new SyncDataSrc(3, logger, Fail.Setup);
      manager.add("bar", ds3);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(3);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setup();

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(3);
      assertThat(manager.listReady).hasSize(0);

      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).index).isEqualTo(1);
      assertThat(errors.get(0).name).isEqualTo("bar");
      assertThat(errors.get(0).err.toString())
          .isEqualTo(
              "com.github.sttk.errs.Err { reason = java.lang.String XXX, file = TestCommonsTest.java, line = 252 }");
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(6);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2 failed");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void setupButRuntimeException() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.SetupByRuntimeException);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.None);
      manager.add("bar", ds2);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(2);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setup();

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(2);
      assertThat(manager.listReady).hasSize(0);

      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).index).isEqualTo(0);
      assertThat(errors.get(0).name).isEqualTo("foo");
      assertThat(errors.get(0).err.toString())
          .isEqualTo(
              "com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccured RuntimeExceptionOccured[], file = AsyncGroupImpl.java, line = 58, cause = java.lang.RuntimeException }");
    } finally {
      manager.close();
    }
  }

  @Test
  void setupButDsIsNull() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      manager.add("foo", null);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(1);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setup();
      assertThat(errors).hasSize(0);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(0);
  }

  @Test
  void setupWithOrderNoDataSrc() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setupWithOrder(List.of("bar", "foo"));
      assertThat(errors).hasSize(0);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(0);
    var iter = logger.iterator();
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void setupWithOrderAndOk() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.None);
      manager.add("bar", ds2);

      var ds3 = new SyncDataSrc(3, logger, Fail.None);
      manager.add("baz", ds3);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(3);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setupWithOrder(List.of("bar", "foo", "xxx"));
      assertThat(errors).hasSize(0);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(3);
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(9);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 3");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 3");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void setupWithOrdrAndFail() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.Setup);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.Setup);
      manager.add("bar", ds2);

      var ds3 = new SyncDataSrc(3, logger, Fail.None);
      manager.add("baz", ds3);

      var ds4 = new SyncDataSrc(4, logger, Fail.None);
      manager.add("qux", ds4);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(4);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setupWithOrder(List.of("qux", "baz", "foo"));

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(4);
      assertThat(manager.listReady).hasSize(0);

      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).index).isEqualTo(2);
      assertThat(errors.get(0).name).isEqualTo("foo");
      assertThat(errors.get(0).err.toString())
          .isEqualTo(
              "com.github.sttk.errs.Err { reason = java.lang.String XXX, file = TestCommonsTest.java, line = 252 }");
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(9);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 4");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 4");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 3");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1 failed");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 3");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 4");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void setupWithOrderContainingDuplicatedNameAndOk() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.None);
      manager.add("bar", ds2);

      var ds3 = new SyncDataSrc(3, logger, Fail.None);
      manager.add("baz", ds3);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(3);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setupWithOrder(List.of("baz", "baz", "foo"));

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(3);

      assertThat(errors).isEmpty();
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(9);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 3");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 3");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void setupWithOrderContainingDuplicatedNameAndOk_2() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.None);
      manager.add("bar", ds2);

      var ds3 = new SyncDataSrc(3, logger, Fail.None);
      manager.add("baz", ds3);

      var ds4 = new SyncDataSrc(4, logger, Fail.None);
      manager.add("qux", ds4);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(4);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setupWithOrder(List.of("baz", "foo", "baz", "qux"));

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(4);

      assertThat(errors).isEmpty();
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(12);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 4");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 3");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 4");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 4");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 3");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void setupWithOrderButOneOfNamesIsNotUsed() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.None);
      manager.add("bar", ds2);

      var ds3 = new SyncDataSrc(3, logger, Fail.None);
      manager.add("baz", ds3);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(3);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setupWithOrder(List.of("baz", "foo", "xxx"));

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(3);

      assertThat(errors).isEmpty();
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(9);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 3");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 3");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void setupWithOrderButOneOfNamesIsNotUsed_2() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.None);
      manager.add("bar", ds2);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(2);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setupWithOrder(List.of("baz", "xxx", "foo"));

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(2);
      assertThat(errors).isEmpty();
    } finally {
      manager.close();
    }
  }

  @Test
  void setupWithOrderButRuntimeException() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.SetupByRuntimeException);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.None);
      manager.add("bar", ds2);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(2);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setupWithOrder(List.of("bar", "foo"));

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(2);
      assertThat(manager.listReady).hasSize(0);

      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).index).isEqualTo(1);
      assertThat(errors.get(0).name).isEqualTo("foo");
      assertThat(errors.get(0).err.toString())
          .isEqualTo(
              "com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccured RuntimeExceptionOccured[], file = AsyncGroupImpl.java, line = 58, cause = java.lang.RuntimeException }");
    } finally {
      manager.close();
    }
  }

  @Test
  void setupWithOrderButDsIsNull() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      manager.add("foo", null);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(1);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setupWithOrder(List.of("xxx", "foo"));
      assertThat(errors).hasSize(0);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);
    } finally {
      manager.close();
    }
  }

  @Test
  void testCopyDsReadyToMap() {
    var logger = new ArrayList<String>();

    var contMap = new HashMap<String, DataSrcContainer>();

    var manager = new DataSrcManager(true);
    try {
      manager.copyDsReadyToMap(contMap);
    } finally {
      manager.close();
    }
    assertThat(contMap).isEmpty();

    manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("foo", ds1);

      var errors = manager.setup();
      assertThat(errors).isEmpty();

      manager.copyDsReadyToMap(contMap);
    } finally {
      manager.close();
    }
    assertThat(contMap).hasSize(1);
    assertThat(contMap.get("foo").local).isTrue();
    assertThat(contMap.get("foo").name).isEqualTo("foo");

    manager = new DataSrcManager(false);
    try {
      var ds2 = new AsyncDataSrc(2, logger, Fail.None);
      var ds3 = new SyncDataSrc(3, logger, Fail.None);
      manager.add("bar", ds2);
      manager.add("baz", ds3);

      var errors = manager.setup();
      assertThat(errors).isEmpty();

      manager.copyDsReadyToMap(contMap);
    } finally {
      manager.close();
    }
    assertThat(contMap).hasSize(3);
    assertThat(contMap.get("foo").local).isTrue();
    assertThat(contMap.get("foo").name).isEqualTo("foo");
    assertThat(contMap.get("bar").local).isFalse();
    assertThat(contMap.get("bar").name).isEqualTo("bar");
    assertThat(contMap.get("baz").local).isFalse();
    assertThat(contMap.get("baz").name).isEqualTo("baz");
  }

  @Test
  void add_copyDsReadyToMap_remove_copyDsReadyToMap() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.None);
      manager.add("bar", ds2);

      var errors = manager.setup();
      assertThat(errors).isEmpty();

      var contMap = new HashMap<String, DataSrcContainer>();
      manager.copyDsReadyToMap(contMap);
      assertThat(contMap).hasSize(2);
      assertThat(contMap.get("foo").local).isTrue();
      assertThat(contMap.get("foo").name).isEqualTo("foo");
      assertThat(contMap.get("foo").ds).isNotNull();
      assertThat(contMap.get("bar").local).isTrue();
      assertThat(contMap.get("bar").name).isEqualTo("bar");
      assertThat(contMap.get("bar").ds).isNotNull();

      manager.remove("foo");
      errors = manager.setup();
      assertThat(errors).isEmpty();

      contMap = new HashMap<String, DataSrcContainer>();
      manager.copyDsReadyToMap(contMap);
      assertThat(contMap).hasSize(1);
      assertThat(contMap.get("bar").local).isTrue();
      assertThat(contMap.get("bar").name).isEqualTo("bar");
      assertThat(contMap.get("bar").ds).isNotNull();
    } finally {
      manager.close();
    }
  }
}
