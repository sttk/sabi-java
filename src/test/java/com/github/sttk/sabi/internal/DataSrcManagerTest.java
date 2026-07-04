package com.github.sttk.sabi.internal;

import static com.github.sttk.sabi.internal.DataConnManagerTest.AsyncDataConn;
import static com.github.sttk.sabi.internal.DataConnManagerTest.SyncDataConn;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.DataSrc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DataSrcManagerTest {
  private DataSrcManagerTest() {}

  static enum Fail {
    Not,
    Setup,
    CreateDataConn,
  }

  static class SyncDataSrc implements DataSrc {
    int id;
    List<String> logger;
    Fail fail;

    SyncDataSrc(int id, List<String> logger, Fail fail) {
      logger.add(String.format("SyncDataSrc#new %d", id));
      this.id = id;
      this.logger = logger;
      this.fail = fail;
    }

    @Override
    public void setup(AsyncGroup ag) throws Err {
      if (this.fail == Fail.Setup) {
        this.logger.add(String.format("SyncDataSrc#setup %d failed", this.id));
        throw new Err("XXX");
      }
      this.logger.add(String.format("SyncDataSrc#setup %d", this.id));
    }

    @Override
    public void close() {
      this.logger.add(String.format("SyncDataSrc#close %d", this.id));
    }

    @Override
    public DataConn createDataConn() throws Err {
      if (this.fail == Fail.CreateDataConn) {
        this.logger.add(String.format("SyncDataSrc#createDataConn %d failed", this.id));
        throw new Err("XXX");
      }
      this.logger.add(String.format("SyncDataSrc#createDataConn %d", this.id));
      return new SyncDataConn(this.id, this.logger, DataConnManagerTest.Fail.Not);
    }
  }

  static class AsyncDataSrc implements DataSrc {
    int id;
    List<String> logger;
    Fail fail;

    AsyncDataSrc(int id, List<String> logger, Fail fail) {
      logger.add(String.format("AsyncDataSrc#new %d", id));
      this.id = id;
      this.logger = logger;
      this.fail = fail;
    }

    @Override
    public void setup(AsyncGroup ag) throws Err {
      ag.add(
          () -> {
            if (this.fail == Fail.Setup) {
              this.logger.add(String.format("AsyncDataSrc#setup %d failed", this.id));
              throw new Err("XXX");
            }
            this.logger.add(String.format("AsyncDataSrc#setup %d", this.id));
          });
    }

    @Override
    public void close() {
      this.logger.add(String.format("AsyncDataSrc#close %d", this.id));
    }

    @Override
    public DataConn createDataConn() throws Err {
      if (this.fail == Fail.CreateDataConn) {
        this.logger.add(String.format("AsyncDataSrc#createDataConn %d failed", this.id));
        throw new Err("XXX");
      }
      this.logger.add(String.format("AsyncDataSrc#createDataConn %d", this.id));
      return new AsyncDataConn(this.id, this.logger, DataConnManagerTest.Fail.Not);
    }
  }

  ///

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
      var ds1 = new SyncDataSrc(1, logger, Fail.Not);
      manager.add("foo", ds1);

      assertThat(manager.local).isTrue();
      assertThat(manager.listUnready).hasSize(1);
      assertThat(manager.listReady).hasSize(0);

      assertThat(manager.listUnready.get(0).name).isEqualTo("foo");

      var ds2 = new AsyncDataSrc(2, logger, Fail.Not);
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
      var ds1 = new SyncDataSrc(1, logger, Fail.Not);
      manager.add("foo", ds1);

      var ds2 = new AsyncDataSrc(2, logger, Fail.Not);
      manager.add("bar", ds2);

      var errors = manager.setup();
      assertThat(errors).hasSize(0);

      var ds3 = new SyncDataSrc(3, logger, Fail.Not);
      manager.add("baz", ds3);

      var ds4 = new AsyncDataSrc(4, logger, Fail.Not);
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
      var ds1 = new SyncDataSrc(1, logger, Fail.Not);
      manager.add("foo", ds1);

      var ds2 = new AsyncDataSrc(2, logger, Fail.Not);
      manager.add("bar", ds2);

      var errors = manager.setup();
      assertThat(errors).hasSize(0);

      var ds3 = new SyncDataSrc(3, logger, Fail.Not);
      manager.add("baz", ds3);

      var ds4 = new AsyncDataSrc(4, logger, Fail.Not);
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
      var ds1 = new SyncDataSrc(1, logger, Fail.Not);
      manager.add("foo", ds1);

      var ds2 = new AsyncDataSrc(2, logger, Fail.Not);
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
      var ds1 = new SyncDataSrc(1, logger, Fail.Not);
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
              "com.github.sttk.errs.Err { reason = java.lang.String XXX, file = DataSrcManagerTest.java, line = 41 }");
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
  void setupWithOrderNoDtaSrc() {
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
      var ds1 = new SyncDataSrc(1, logger, Fail.Not);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.Not);
      manager.add("bar", ds2);

      var ds3 = new SyncDataSrc(3, logger, Fail.Not);
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

      var ds3 = new SyncDataSrc(3, logger, Fail.Not);
      manager.add("baz", ds3);

      var ds4 = new SyncDataSrc(4, logger, Fail.Not);
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
              "com.github.sttk.errs.Err { reason = java.lang.String XXX, file = DataSrcManagerTest.java, line = 41 }");
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
      var ds1 = new SyncDataSrc(1, logger, Fail.Not);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.Not);
      manager.add("bar", ds2);

      var ds3 = new SyncDataSrc(3, logger, Fail.Not);
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
  void setupWithOrderContainingDuplicatedNameAndOk2() {
    var logger = new ArrayList<String>();

    var manager = new DataSrcManager(true);
    try {
      var ds1 = new SyncDataSrc(1, logger, Fail.Not);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.Not);
      manager.add("bar", ds2);

      var ds3 = new SyncDataSrc(3, logger, Fail.Not);
      manager.add("baz", ds3);

      var ds4 = new SyncDataSrc(4, logger, Fail.Not);
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
      var ds1 = new SyncDataSrc(1, logger, Fail.Not);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.Not);
      manager.add("bar", ds2);

      var ds3 = new SyncDataSrc(3, logger, Fail.Not);
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
      var ds1 = new SyncDataSrc(1, logger, Fail.Not);
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
      var ds2 = new AsyncDataSrc(2, logger, Fail.Not);
      var ds3 = new SyncDataSrc(3, logger, Fail.Not);
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
}
