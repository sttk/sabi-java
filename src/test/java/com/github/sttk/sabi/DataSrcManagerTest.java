package com.github.sttk.sabi;

import static com.github.sttk.sabi.TestDoubles.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DataSrcManagerTest {
  private DataSrcManagerTest() {}

  @Nested
  class NewAddRemoveClose {
    @Test
    void testNew() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isFalse();
      assertThat(manager.listUnready).isEmpty();
      assertThat(manager.listReady).isEmpty();

      manager = new DataSrcManager(false);
      assertThat(manager.local).isFalse();
      assertThat(manager.isAbnormal).isFalse();
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
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(1);
        assertThat(manager.listReady).hasSize(0);

        assertThat(manager.listUnready.get(0).name).isEqualTo("foo");

        var ds2 = new AsyncDataSrc(2, logger, Fail.None);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
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
    void dontAddIfAbnormalState() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      manager.isAbnormal = true;
      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.None);
        manager.add("foo", ds1);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isTrue();
        assertThat(manager.listUnready).hasSize(0);
        assertThat(manager.listReady).hasSize(0);

        var ds2 = new AsyncDataSrc(2, logger, Fail.None);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isTrue();
        assertThat(manager.listUnready).hasSize(0);
        assertThat(manager.listReady).hasSize(0);
      } finally {
        manager.close();
      }
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

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(0);

        manager.remove("foo");
        manager.remove("bar");
      } finally {
        // manager.close(); // to see Close logs by remove
      }

      assertThat(logger).hasSize(2);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataSrc#new 2");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testRemoveButOnlyCloseDataSrcsInReadyList() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.None);
        manager.listUnready.add(new DataSrcContainer(true, "foo", ds1));

        var ds2 = new AsyncDataSrc(2, logger, Fail.None);
        manager.listUnready.add(new DataSrcContainer(true, "bar", ds2));

        var ds3 = new SyncDataSrc(3, logger, Fail.None);
        manager.listReady.add(new DataSrcContainer(true, "baz", ds3));

        var ds4 = new AsyncDataSrc(4, logger, Fail.None);
        manager.listReady.add(new DataSrcContainer(true, "qux", ds4));

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(2);

        manager.remove("baz");
        manager.remove("foo");
        manager.remove("qux");
        manager.remove("bar");
      } finally {
        // manager.close(); // to see Close logs by remove
      }

      assertThat(logger).hasSize(6);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
      assertThat(iter.next()).isEqualTo("AsyncDataSrc#new 4");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 3");
      assertThat(iter.next()).isEqualTo("AsyncDataSrc#close 4");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testRemoveNoDsContainersInUnreadyListToo() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("foo", null);
        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(1);
        assertThat(manager.listReady).hasSize(0);

        manager.remove("foo");
        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(0);
        assertThat(manager.listReady).hasSize(0);
      } finally {
        // manager.close(); // to see Close logs by remove
      }

      assertThat(logger).hasSize(0);
      var iter = logger.iterator();
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testClose() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);

      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.listUnready.add(new DataSrcContainer(true, "foo", ds1));

      var ds2 = new SyncDataSrc(2, logger, Fail.None);
      manager.listUnready.add(new DataSrcContainer(true, "bar", ds2));

      var ds3 = new SyncDataSrc(3, logger, Fail.None);
      manager.listReady.add(new DataSrcContainer(true, "baz", ds3));

      var ds4 = new SyncDataSrc(4, logger, Fail.None);
      manager.listReady.add(new DataSrcContainer(true, "qux", ds4));

      manager.close();

      assertThat(logger).hasSize(6);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 4");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 4");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 3");
      assertThat(iter.hasNext()).isFalse();
    }
  }

  @Nested
  class Utils {
    @Test
    void copyDsReadyToMapWithAddAndSetup() {
      var logger = new ArrayList<String>();

      var indexMap = new HashMap<String, DataSrcContainer>();

      var manager = new DataSrcManager(true);
      manager.copyDsReadyToMap(indexMap);
      assertThat(indexMap).isEmpty();

      manager = new DataSrcManager(true);

      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("foo", ds1);

      var errors = manager.setup();
      assertThat(errors).isEmpty();

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isFalse();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(1);

      manager.copyDsReadyToMap(indexMap);
      assertThat(indexMap).hasSize(1);

      var cont = indexMap.get("foo");
      assertThat(cont.local).isTrue();
      assertThat(cont.name).isEqualTo("foo");
      assertThat(cont.ds).isEqualTo(ds1);

      manager = new DataSrcManager(false);

      var ds2 = new SyncDataSrc(1, logger, Fail.None);
      var ds3 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("bar", ds2);
      manager.add("baz", ds3);

      errors = manager.setup();
      assertThat(errors).isEmpty();

      manager.copyDsReadyToMap(indexMap);
      assertThat(indexMap).hasSize(3);
      cont = indexMap.get("foo");
      assertThat(cont.local).isTrue();
      assertThat(cont.name).isEqualTo("foo");
      assertThat(cont.ds).isEqualTo(ds1);
      cont = indexMap.get("bar");
      assertThat(cont.local).isFalse();
      assertThat(cont.name).isEqualTo("bar");
      assertThat(cont.ds).isEqualTo(ds2);
      cont = indexMap.get("baz");
      assertThat(cont.local).isFalse();
      assertThat(cont.name).isEqualTo("baz");
      assertThat(cont.ds).isEqualTo(ds3);
    }

    @Test
    void copyDsReadyToMapWithAddAndRemoveAndSetup() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);

      var ds1 = new SyncDataSrc(1, logger, Fail.None);
      manager.add("foo", ds1);

      var ds2 = new SyncDataSrc(2, logger, Fail.None);
      manager.add("bar", ds2);

      var errors = manager.setup();
      assertThat(errors).isEmpty();

      var indexMap = new HashMap<String, DataSrcContainer>();
      manager.copyDsReadyToMap(indexMap);
      assertThat(indexMap).hasSize(2);
      var cont = indexMap.get("foo");
      assertThat(cont.local).isTrue();
      assertThat(cont.name).isEqualTo("foo");
      assertThat(cont.ds).isEqualTo(ds1);
      cont = indexMap.get("bar");
      assertThat(cont.local).isTrue();
      assertThat(cont.name).isEqualTo("bar");
      assertThat(cont.ds).isEqualTo(ds2);

      manager.remove("foo");

      errors = manager.setup();
      assertThat(errors).isEmpty();

      indexMap = new HashMap<String, DataSrcContainer>();
      manager.copyDsReadyToMap(indexMap);
      assertThat(indexMap).hasSize(1);
      cont = indexMap.get("bar");
      assertThat(cont.local).isTrue();
      assertThat(cont.name).isEqualTo("bar");
      assertThat(cont.ds).isEqualTo(ds2);
    }
  }

  @Nested
  class Setup {
    @Test
    void testSetupNoDs() {
      var manager = new DataSrcManager(true);

      var errors = manager.setup();
      assertThat(errors).isEmpty();
    }

    @Test
    void testSetupAndOk() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);

      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.None);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.None);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(0);

        var errors = manager.setup();
        assertThat(errors).isEmpty();

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(0);
        assertThat(manager.listReady).hasSize(2);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(6);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupButFail() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);

      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.None);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.Setup);
        manager.add("bar", ds2);

        var ds3 = new SyncDataSrc(3, logger, Fail.Setup);
        manager.add("baz", ds3);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(3);
        assertThat(manager.listReady).hasSize(0);

        var errors = manager.setup();
        assertThat(errors).hasSize(1);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(3);
        assertThat(manager.listReady).hasSize(0);

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).index).isEqualTo(1);
        assertThat(errors.get(0).name).isEqualTo("bar");
        assertThat(errors.get(0).err.toString())
            .isEqualTo(
                "com.github.sttk.errs.Err { reason = java.lang.String XXX, file = TestDoubles.java, line = 318 }");
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(7);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2 failed");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupAndContainsNoDs() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("foo", null);

        var ds2 = new SyncDataSrc(2, logger, Fail.None);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(0);

        var errors = manager.setup();
        assertThat(errors).hasSize(0);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(0);
        assertThat(manager.listReady).hasSize(1);
      } catch (Exception e) {
        fail(e);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(3);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupAndContainsNoDsButAnotherDsFail() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("foo", null);

        var ds2 = new SyncDataSrc(2, logger, Fail.Setup);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(0);

        var errors = manager.setup();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).index).isEqualTo(1);
        assertThat(errors.get(0).name).isEqualTo("bar");
        assertThat(errors.get(0).err.toString())
            .isEqualTo(
                "com.github.sttk.errs.Err { reason = java.lang.String XXX, file = TestDoubles.java, line = 318 }");

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(0);
      } catch (Exception e) {
        fail(e);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(3);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2 failed");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupAndContainsDuplicatedName() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);

      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.None);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.None);
        manager.add("foo", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(0);

        var errors = manager.setup();
        assertThat(errors).isEmpty();

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(0);
        assertThat(manager.listReady).hasSize(2);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(6);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderNoDs() {
      var manager = new DataSrcManager(true);

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isFalse();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      var errors = manager.setupWithOrder(List.of("foo", "bar"));
      assertThat(errors).isEmpty();

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isFalse();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);
    }

    @Test
    void testSetupWithOrderAndOk() {
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
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(3);
        assertThat(manager.listReady).hasSize(0);

        var errors = manager.setupWithOrder(List.of("foo", "bar"));
        assertThat(errors).isEmpty();

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
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
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 3");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 3");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderButFail() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.None);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.Setup);
        manager.add("bar", ds2);

        var ds3 = new SyncDataSrc(3, logger, Fail.Setup);
        manager.add("baz", ds3);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(3);
        assertThat(manager.listReady).hasSize(0);

        var errors = manager.setupWithOrder(List.of("foo", "bar"));
        assertThat(errors).hasSize(1);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(3);
        assertThat(manager.listReady).hasSize(0);

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).index).isEqualTo(1);
        assertThat(errors.get(0).name).isEqualTo("bar");
        assertThat(errors.get(0).err.toString())
            .isEqualTo(
                "com.github.sttk.errs.Err { reason = java.lang.String XXX, file = TestDoubles.java, line = 318 }");
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(7);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2 failed");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderAndContainsNoDs() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("foo", null);

        var ds2 = new SyncDataSrc(2, logger, Fail.None);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(0);

        var errors = manager.setupWithOrder(List.of("foo", "bar"));
        assertThat(errors).hasSize(0);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(0);
        assertThat(manager.listReady).hasSize(1);
      } catch (Exception e) {
        fail(e);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(3);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderAndContainsNoDsButAnotherDsFail() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("foo", null);

        var ds2 = new SyncDataSrc(2, logger, Fail.Setup);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(0);

        var errors = manager.setupWithOrder(List.of("foo", "bar"));
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).index).isEqualTo(1);
        assertThat(errors.get(0).name).isEqualTo("bar");
        assertThat(errors.get(0).err.toString())
            .isEqualTo(
                "com.github.sttk.errs.Err { reason = java.lang.String XXX, file = TestDoubles.java, line = 318 }");

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(0);
      } catch (Exception e) {
        fail(e);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(3);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2 failed");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderAndContainsDuplicatedName() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.None);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.None);
        manager.add("foo", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(0);

        var errors = manager.setupWithOrder(List.of("foo"));
        assertThat(errors).isEmpty();

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(0);
        assertThat(manager.listReady).hasSize(2);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(6);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderAndArgsContainsDuplicatdNames() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.None);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.None);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(0);

        var errors = manager.setupWithOrder(List.of("foo", "foo"));
        assertThat(errors).isEmpty();

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(0);
        assertThat(manager.listReady).hasSize(2);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(6);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderAndArgsContainsDuplicatdNames2() {
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
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(3);
        assertThat(manager.listReady).hasSize(0);

        var errors = manager.setupWithOrder(List.of("baz", "baz", "foo"));
        assertThat(errors).isEmpty();

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
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
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 3");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 3");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderAndOneOfNamsIsNotUsed() {
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
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(3);
        assertThat(manager.listReady).hasSize(0);

        var errors = manager.setupWithOrder(List.of("baz", "foo", "xxx"));
        assertThat(errors).isEmpty();

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
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
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 3");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 3");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderAndOneOfNamsIsNotUsed2() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.None);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.None);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(0);

        var errors = manager.setupWithOrder(List.of("baz", "foo", "xxx"));
        assertThat(errors).isEmpty();

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(0);
        assertThat(manager.listReady).hasSize(2);

      } finally {
        manager.close();
      }
    }
  }

  @Nested
  class Fatal {
    @Test
    void testRemoveButCloseError() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.CloseDsError);
        manager.listReady.add(new DataSrcContainer(true, "foo", ds1));
        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(0);
        assertThat(manager.listReady).hasSize(1);

        manager.remove("foo");
        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(0);
        assertThat(manager.listReady).hasSize(0);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("close ds Error");
      } finally {
        // manager.close(); // to see Close logs by remove
      }

      assertThat(logger).hasSize(2);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1 Error");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testRemoveButCloseRuntimeException() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.CloseDsRuntimeException);
        manager.listReady.add(new DataSrcContainer(true, "foo", ds1));

        var ds2 = new SyncDataSrc(2, logger, Fail.CloseDsRuntimeException);
        manager.listReady.add(new DataSrcContainer(true, "foo", ds2));

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(0);
        assertThat(manager.listReady).hasSize(2);

        manager.remove("foo");
        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(0);
        assertThat(manager.listReady).hasSize(0);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("close ds RuntimeException");
      } finally {
        // manager.close(); // to see Close logs by remove
      }

      assertThat(logger).hasSize(4);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1 RuntimeException");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2 RuntimeException");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testCloseButError() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.CloseDsError);
        manager.listReady.add(new DataSrcContainer(true, "foo", ds1));

        var ds2 = new SyncDataSrc(2, logger, Fail.None);
        manager.listReady.add(new DataSrcContainer(true, "bar", ds2));

        manager.close();
        fail();
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("close ds Error");
      }

      assertThat(logger).hasSize(4);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1 Error");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testCloseButRuntimeException() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.CloseDsRuntimeException);
        manager.listReady.add(new DataSrcContainer(true, "foo", ds1));

        var ds2 = new SyncDataSrc(2, logger, Fail.CloseDsRuntimeException);
        manager.listReady.add(new DataSrcContainer(true, "bar", ds2));

        manager.close();
        fail();
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("close ds RuntimeException");
      }

      assertThat(logger).hasSize(4);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2 RuntimeException");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1 RuntimeException");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupButError() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.SetupError);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.None);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(0);

        manager.setup();
        fail();
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("setup Error");
      }

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      assertThat(logger).hasSize(4);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1 Error");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupButRuntimeException() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        var ds1 = new SyncDataSrc(1, logger, Fail.None);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.SetupRuntimeException);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(2);
        assertThat(manager.listReady).hasSize(0);

        manager.setup();
        fail();
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("setup RuntimeException");
      }

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      assertThat(logger).hasSize(6);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2 RuntimeException");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupButCloseError() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("xxx", null);

        var ds1 = new SyncDataSrc(1, logger, Fail.CloseDsError);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.SetupError);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(3);
        assertThat(manager.listReady).hasSize(0);

        manager.setup();
        fail();
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("setup Error");
      }

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      assertThat(logger).hasSize(6);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2 Error");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1 Error");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupButCloseRuntimeException() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("xxx", null);

        var ds1 = new SyncDataSrc(1, logger, Fail.CloseDsRuntimeException);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.SetupRuntimeException);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(3);
        assertThat(manager.listReady).hasSize(0);

        manager.setup();
        fail();
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("setup RuntimeException");
      }

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      assertThat(logger).hasSize(6);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2 RuntimeException");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1 RuntimeException");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupFailAndCloseError() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("xxx", null);

        var ds1 = new SyncDataSrc(1, logger, Fail.CloseDsError);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.CloseDsError);
        manager.add("baz", ds2);

        var ds3 = new SyncDataSrc(3, logger, Fail.Setup);
        manager.add("bar", ds3);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(4);
        assertThat(manager.listReady).hasSize(0);

        manager.setup();
        fail();
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("close ds Error");
      }

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      assertThat(logger).hasSize(9);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 3 failed");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 3");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2 Error");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1 Error");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupFailAndCloseRuntimeException() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("xxx", null);

        var ds1 = new SyncDataSrc(1, logger, Fail.CloseDsRuntimeException);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.Setup);
        manager.add("bar", ds2);

        var ds3 = new SyncDataSrc(3, logger, Fail.CloseDsRuntimeException);
        manager.add("baz", ds3);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(4);
        assertThat(manager.listReady).hasSize(0);

        manager.setup();
        fail();
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("close ds RuntimeException");
      }

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      assertThat(logger).hasSize(7);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2 failed");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1 RuntimeException");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderButError() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("xxx", null);

        var ds1 = new SyncDataSrc(1, logger, Fail.SetupError);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.None);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(3);
        assertThat(manager.listReady).hasSize(0);

        manager.setupWithOrder(List.of("bar"));
        fail();
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("setup Error");
      }

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      assertThat(logger).hasSize(6);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1 Error");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderButRuntimeException() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("xxx", null);

        var ds1 = new SyncDataSrc(1, logger, Fail.SetupRuntimeException);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.None);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(3);
        assertThat(manager.listReady).hasSize(0);

        manager.setupWithOrder(List.of("bar"));
        fail();
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("setup RuntimeException");
      }

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      assertThat(logger).hasSize(6);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1 RuntimeException");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderButCloseError() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("xxx", null);

        var ds1 = new SyncDataSrc(1, logger, Fail.SetupError);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.CloseDsError);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(3);
        assertThat(manager.listReady).hasSize(0);

        manager.setupWithOrder(List.of("bar"));
        fail();
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("setup Error");
      }

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      assertThat(logger).hasSize(6);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1 Error");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2 Error");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderButCloseRuntimeException() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("xxx", null);

        var ds1 = new SyncDataSrc(1, logger, Fail.SetupRuntimeException);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.CloseDsRuntimeException);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(3);
        assertThat(manager.listReady).hasSize(0);

        manager.setupWithOrder(List.of("bar"));
        fail();
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("setup RuntimeException");
      }

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      assertThat(logger).hasSize(6);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1 RuntimeException");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2 RuntimeException");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderFailButCloseError() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("xxx", null);

        var ds1 = new SyncDataSrc(1, logger, Fail.Setup);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.CloseDsError);
        manager.add("bar", ds2);

        var ds3 = new SyncDataSrc(3, logger, Fail.CloseDsError);
        manager.add("baz", ds3);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(4);
        assertThat(manager.listReady).hasSize(0);

        manager.setupWithOrder(List.of("bar", "baz"));
        fail();
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("close ds Error");
      }

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      assertThat(logger).hasSize(9);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 3");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 3");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1 failed");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 3 Error");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2 Error");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testSetupWithOrderFailButCloseRuntimeException() {
      var logger = new ArrayList<String>();

      var manager = new DataSrcManager(true);
      try {
        manager.add("xxx", null);

        var ds1 = new SyncDataSrc(1, logger, Fail.Setup);
        manager.add("foo", ds1);

        var ds2 = new SyncDataSrc(2, logger, Fail.CloseDsRuntimeException);
        manager.add("bar", ds2);

        assertThat(manager.local).isTrue();
        assertThat(manager.isAbnormal).isFalse();
        assertThat(manager.listUnready).hasSize(3);
        assertThat(manager.listReady).hasSize(0);

        manager.setupWithOrder(List.of("bar"));
        fail();
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("close ds RuntimeException");
      }

      assertThat(manager.local).isTrue();
      assertThat(manager.isAbnormal).isTrue();
      assertThat(manager.listUnready).hasSize(0);
      assertThat(manager.listReady).hasSize(0);

      assertThat(logger).hasSize(6);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1 failed");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
      assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2 RuntimeException");
      assertThat(iter.hasNext()).isFalse();
    }
  }
}
