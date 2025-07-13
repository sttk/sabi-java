package com.github.sttk.sabi.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.DataHub;
import com.github.sttk.sabi.DataSrc;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DataHubInnerTest {
  private DataHubInnerTest() {}

  static final int FAIL__NOT = 0;
  static final int FAIL__SETUP = 1;
  static final int FAIL__CREATE_DATA_CONN = 2;
  static final int FAIL__COMMIT = 3;
  static final int FAIL__PRE_COMMIT = 4;

  final void suppressWarnings_unused(Object a) {}

  static class SyncDataSrc implements DataSrc {
    private int id;
    private int fail;
    private List<String> logger;

    SyncDataSrc(int id, int fail, List<String> logger) {
      this.id = id;
      this.fail = fail;
      this.logger = logger;
    }

    @Override
    public void setup(AsyncGroup ag) throws Exc {
      if (this.fail == FAIL__SETUP) {
        this.logger.add(String.format("SyncDataSrc %d failed to setup", this.id));
        throw new Exc("XXX");
      }
      this.logger.add(String.format("SyncDataSrc %d setupped", this.id));
    }

    @Override
    public void close() {
      this.logger.add(String.format("SyncDataSrc %d closed", this.id));
    }

    @Override
    public DataConn createDataConn() throws Exc {
      if (this.fail == FAIL__CREATE_DATA_CONN) {
        this.logger.add(String.format("SyncDataSrc %d failed to create a DataConn", this.id));
        throw new Exc("xxx");
      }
      this.logger.add(String.format("SyncDataSrc %d created DataConn", this.id));
      var conn = new SyncDataConn(this.id, this.fail, this.logger);
      return conn;
    }
  }

  static class AsyncDataSrc implements DataSrc {
    private int id;
    private int fail;
    private List<String> logger;

    AsyncDataSrc(int id, int fail, List<String> logger) {
      this.id = id;
      this.fail = fail;
      this.logger = logger;
    }

    @Override
    public void setup(AsyncGroup ag) throws Exc {
      ag.add(
          () -> {
            try {
              Thread.sleep(50);
            } catch (Exception e) {
            }
            if (this.fail == FAIL__SETUP) {
              this.logger.add(String.format("AsyncDataSrc %d failed to setup", this.id));
              throw new Exc("YYY");
            }
            this.logger.add(String.format("AsyncDataSrc %d setupped", this.id));
          });
    }

    @Override
    public void close() {
      this.logger.add(String.format("AsyncDataSrc %d closed", this.id));
    }

    @Override
    public DataConn createDataConn() throws Exc {
      if (this.fail == FAIL__CREATE_DATA_CONN) {
        this.logger.add(String.format("AsyncDataSrc %d failed to create a DataConn", this.id));
        throw new Exc("yyy");
      }
      this.logger.add(String.format("AsyncDataSrc %d created DataConn", this.id));
      var conn = new AsyncDataConn(this.id, this.fail, this.logger);
      return conn;
    }
  }

  static class SyncDataConn implements DataConn {
    private int id;
    private int fail;
    private boolean committed;
    private List<String> logger;

    SyncDataConn(int id, int fail, List<String> logger) {
      this.id = id;
      this.fail = fail;
      this.logger = logger;
    }

    @Override
    public void commit(AsyncGroup ag) throws Exc {
      if (this.fail == FAIL__COMMIT) {
        this.logger.add(String.format("SyncDataConn %d failed to commit", this.id));
        throw new Exc("ZZZ");
      }
      this.committed = true;
      this.logger.add(String.format("SyncDataConn %d committed", this.id));
    }

    @Override
    public void preCommit(AsyncGroup ag) throws Exc {
      if (this.fail == FAIL__PRE_COMMIT) {
        this.logger.add(String.format("SyncDataConn %d failed to pre commit", this.id));
        throw new Exc("zzz");
      }
      this.logger.add(String.format("SyncDataConn %d pre committed", this.id));
    }

    @Override
    public void postCommit(AsyncGroup ag) {
      this.logger.add(String.format("SyncDataConn %d post committed", this.id));
    }

    @Override
    public boolean shouldForceBack() {
      return this.committed;
    }

    @Override
    public void rollback(AsyncGroup ag) {
      this.logger.add(String.format("SyncDataConn %d rollbacked", this.id));
    }

    @Override
    public void forceBack(AsyncGroup ag) {
      this.logger.add(String.format("SyncDataConn %d forced back", this.id));
    }

    @Override
    public void close() {
      this.logger.add(String.format("SyncDataConn %d closed", this.id));
    }
  }

  static class AsyncDataConn implements DataConn {
    private int id;
    private int fail;
    private boolean committed;
    private List<String> logger;

    AsyncDataConn(int id, int fail, List<String> logger) {
      this.id = id;
      this.fail = fail;
      this.logger = logger;
    }

    @Override
    public void commit(AsyncGroup ag) throws Exc {
      if (this.fail == FAIL__COMMIT) {
        this.logger.add(String.format("AsyncDataConn %d failed to commit", this.id));
        throw new Exc("VVV");
      }
      this.committed = true;
      this.logger.add(String.format("AsyncDataConn %d committed", this.id));
    }

    @Override
    public void preCommit(AsyncGroup ag) throws Exc {
      if (this.fail == FAIL__PRE_COMMIT) {
        this.logger.add(String.format("AsyncDataConn %d failed to pre commit", this.id));
        throw new Exc("vvv");
      }
      this.logger.add(String.format("AsyncDataConn %d pre committed", this.id));
    }

    @Override
    public void postCommit(AsyncGroup ag) {
      this.logger.add(String.format("AsyncDataConn %d post committed", this.id));
    }

    @Override
    public boolean shouldForceBack() {
      return this.committed;
    }

    @Override
    public void rollback(AsyncGroup ag) {
      this.logger.add(String.format("AsyncDataConn %d rollbacked", this.id));
    }

    @Override
    public void forceBack(AsyncGroup ag) {
      this.logger.add(String.format("AsyncDataConn %d forced back", this.id));
    }

    @Override
    public void close() {
      this.logger.add(String.format("AsyncDataConn %d closed", this.id));
    }
  }

  static void resetGlobalVariables() {
    DataHubInner.GLOBAL_DATA_SRCS_FIXED.set(false);
    DataHubInner.GLOBAL_DATA_SRC_LIST.closeDataSrcs();
  }

  @Nested
  class TestOfGlobalFunctions {
    @BeforeEach
    void beforeEach() {
      resetGlobalVariables();
    }

    @AfterEach
    void afterEach() {
      resetGlobalVariables();
    }

    @Test
    void setup_and_shutdown() {
      var logger = new ArrayList<String>();

      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();
      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead).isNull();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      var ptr = DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead;
      assertThat(ptr).isNotNull();
      assertThat(ptr.name).isEqualTo("foo");
      ptr = ptr.next;
      assertThat(ptr).isNotNull();
      assertThat(ptr.name).isEqualTo("bar");
      ptr = ptr.next;
      assertThat(ptr).isNull();

      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead).isNull();

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();

        ptr = DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("foo");
        ptr = ptr.next;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("bar");
        ptr = ptr.next;
        assertThat(ptr).isNull();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();
      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead).isNull();

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void fail_to_setup() {
      var logger = new ArrayList<String>();

      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();
      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead).isNull();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__SETUP, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__SETUP, logger));

      var ptr = DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead;
      assertThat(ptr).isNotNull();
      assertThat(ptr.name).isEqualTo("foo");
      ptr = ptr.next;
      assertThat(ptr).isNotNull();
      assertThat(ptr.name).isEqualTo("bar");
      ptr = ptr.next;
      assertThat(ptr).isNull();

      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead).isNull();

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        fail();
      } catch (Exc e) {
        switch (e.getReason()) {
          case DataHub.FailToSetupGlobalDataSrcs r -> {
            assertThat(r.errors()).hasSize(2);
            assertThat(r.errors().get("foo").getReason()).isEqualTo("YYY");
            assertThat(r.errors().get("bar").getReason()).isEqualTo("XXX");
          }
          default -> fail();
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();
      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead).isNull();

      assertThat(logger)
          .containsExactly("SyncDataSrc 2 failed to setup", "AsyncDataSrc 1 failed to setup");
    }

    @Test
    void cannot_add_global_data_srcs_after_setup() {
      var logger = new ArrayList<String>();

      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();
      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead).isNull();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));

      var ptr = DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead;
      assertThat(ptr).isNotNull();
      assertThat(ptr.name).isEqualTo("foo");
      ptr = ptr.next;
      assertThat(ptr).isNull();

      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead).isNull();

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();

        ptr = DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("foo");
        ptr = ptr.next;
        assertThat(ptr).isNull();

        DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

        assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();

        ptr = DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("foo");
        ptr = ptr.next;
        assertThat(ptr).isNull();

      } catch (Exception e) {
        fail(e);
      }

      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();
      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead).isNull();

      assertThat(logger).containsExactly("AsyncDataSrc 1 setupped", "AsyncDataSrc 1 closed");
    }

    @Test
    void do_nothing_if_executing_setup_twice() {
      var logger = new ArrayList<String>();

      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();
      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead).isNull();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));

      var ptr = DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead;
      assertThat(ptr).isNotNull();
      assertThat(ptr.name).isEqualTo("foo");
      ptr = ptr.next;
      assertThat(ptr).isNull();

      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead).isNull();

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();

        ptr = DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("foo");
        ptr = ptr.next;
        assertThat(ptr).isNull();

        DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

        assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();

        ptr = DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("foo");
        ptr = ptr.next;
        assertThat(ptr).isNull();

      } catch (Exception e) {
        fail(e);
      }

      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();
      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead).isNull();

      assertThat(logger).containsExactly("AsyncDataSrc 1 setupped", "AsyncDataSrc 1 closed");
    }
  }

  @Nested
  class TestOfDataHubLocal {
    @BeforeEach
    void beforeEach() {
      resetGlobalVariables();
    }

    @AfterEach
    void afterEach() {
      resetGlobalVariables();
    }

    @Test
    void new_and_close_with_no_global_data_srcs() {
      var hub = new DataHubInner();

      assertThat(hub.localDataSrcList.notSetupHead).isNull();
      assertThat(hub.localDataSrcList.didSetupHead).isNull();
      assertThat(hub.dataConnList.head).isNull();
      assertThat(hub.dataSrcMap).hasSize(0);
      assertThat(hub.dataConnMap).hasSize(0);
      assertThat(hub.fixed).isFalse();

      hub.close();
    }

    @Test
    void new_and_close_with_global_data_srcs() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();

        var ptr = DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("foo");
        ptr = ptr.next;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("bar");
        ptr = ptr.next;
        assertThat(ptr).isNull();

        var hub = new DataHubInner();

        assertThat(hub.localDataSrcList.notSetupHead).isNull();
        assertThat(hub.localDataSrcList.didSetupHead).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(2);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();

        ptr = DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("foo");
        ptr = ptr.next;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("bar");
        ptr = ptr.next;
        assertThat(ptr).isNull();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();
      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead).isNull();

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void uses_and_disuses() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        assertThat(hub.localDataSrcList.notSetupHead).isNull();
        assertThat(hub.localDataSrcList.didSetupHead).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(2);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        hub.uses("baz", new SyncDataSrc(3, FAIL__NOT, logger));
        var ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.localDataSrcList.didSetupHead).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(2);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        hub.uses("qux", new AsyncDataSrc(4, FAIL__NOT, logger));
        ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("qux");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.localDataSrcList.didSetupHead).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(2);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        hub.disuses("foo"); // do nothing because of global
        hub.disuses("bar"); // do nothing because of global

        ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("qux");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.localDataSrcList.didSetupHead).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(2);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        hub.disuses("baz");

        ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("qux");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.localDataSrcList.didSetupHead).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(2);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        hub.disuses("qux");

        ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNull();
        assertThat(hub.localDataSrcList.didSetupHead).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(2);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.notSetupHead).isNull();
      assertThat(DataHubInner.GLOBAL_DATA_SRC_LIST.didSetupHead).isNull();

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 3 closed",
              "AsyncDataSrc 4 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void cannot_add_and_remove_data_src_between_begin_and_end() {
      var logger = new ArrayList<String>();

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        assertThat(hub.localDataSrcList.notSetupHead).isNull();
        assertThat(hub.localDataSrcList.didSetupHead).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(0);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        hub.uses("baz", new SyncDataSrc(1, FAIL__NOT, logger));
        var ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.localDataSrcList.didSetupHead).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(0);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        try {
          hub.begin();
        } catch (Exception e) {
          fail(e);
        }

        ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNull();
        ptr = hub.localDataSrcList.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(1);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isTrue();

        hub.uses("foo", new AsyncDataSrc(2, FAIL__NOT, logger));

        ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNull();
        ptr = hub.localDataSrcList.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(1);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isTrue();

        hub.disuses("baz");

        ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNull();
        ptr = hub.localDataSrcList.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(1);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isTrue();

        hub.end();

        ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNull();
        ptr = hub.localDataSrcList.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(1);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        hub.uses("foo", new AsyncDataSrc(2, FAIL__NOT, logger));

        ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("foo");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        ptr = hub.localDataSrcList.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(1);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        hub.disuses("baz");

        ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("foo");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        ptr = hub.localDataSrcList.didSetupHead;
        assertThat(ptr).isNull();
        assertThat(hub.dataConnList.head).isNull();
        assertThat(hub.dataSrcMap).hasSize(0);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).containsExactly("SyncDataSrc 1 setupped", "SyncDataSrc 1 closed");
    }

    @Test
    void begin_and_end() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new SyncDataSrc(3, FAIL__NOT, logger));
        hub.uses("qux", new AsyncDataSrc(4, FAIL__NOT, logger));

        var ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("qux");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        ptr = hub.localDataSrcList.didSetupHead;
        assertThat(ptr).isNull();
        assertThat(hub.dataSrcMap).hasSize(2);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        try {
          hub.begin();
        } catch (Exception e) {
          fail(e);
        }

        ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNull();
        ptr = hub.localDataSrcList.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("qux");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.dataSrcMap).hasSize(4);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isTrue();

        hub.end();

        ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNull();
        ptr = hub.localDataSrcList.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("qux");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.dataSrcMap).hasSize(4);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 3 setupped",
              "AsyncDataSrc 4 setupped",
              "AsyncDataSrc 4 closed",
              "SyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void begin_and_end_but_fail_sync() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__NOT, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__SETUP, logger));

        try {
          hub.begin();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToSetupLocalDataSrcs rsn -> {
              assertThat(rsn.errors()).hasSize(1);
              var e2 = rsn.errors().get("qux");
              assertThat(e2.getReason()).isEqualTo("XXX");
            }
            default -> fail(e);
          }
        } catch (Exception e) {
          fail(e);
        }

        var ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("qux");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        ptr = hub.localDataSrcList.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.dataSrcMap).hasSize(3);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isTrue();

        hub.end();

        ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("qux");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        ptr = hub.localDataSrcList.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.dataSrcMap).hasSize(3);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 failed to setup",
              "AsyncDataSrc 3 setupped",
              "AsyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void begin_and_end_but_fail_async() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__SETUP, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__NOT, logger));

        try {
          hub.begin();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToSetupLocalDataSrcs rsn -> {
              assertThat(rsn.errors()).hasSize(1);
              var e2 = rsn.errors().get("baz");
              assertThat(e2.getReason()).isEqualTo("YYY");
            }
            default -> fail(e);
          }
        } catch (Exception e) {
          fail(e);
        }

        var ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        ptr = hub.localDataSrcList.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("qux");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.dataSrcMap).hasSize(3);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isTrue();

        hub.end();

        ptr = hub.localDataSrcList.notSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("baz");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        ptr = hub.localDataSrcList.didSetupHead;
        assertThat(ptr).isNotNull();
        assertThat(ptr.name).isEqualTo("qux");
        ptr = ptr.next;
        assertThat(ptr).isNull();
        assertThat(hub.dataSrcMap).hasSize(3);
        assertThat(hub.dataConnMap).hasSize(0);
        assertThat(hub.fixed).isFalse();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 setupped",
              "AsyncDataSrc 3 failed to setup",
              "SyncDataSrc 4 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void commit() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__NOT, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__NOT, logger));

        try {
          hub.begin();

          var conn1 = hub.getDataConn("foo", AsyncDataConn.class);
          assertThat(conn1).isNotNull();

          var conn2 = hub.getDataConn("bar", SyncDataConn.class);
          assertThat(conn2).isNotNull();

          var conn3 = hub.getDataConn("baz", AsyncDataConn.class);
          assertThat(conn3).isNotNull();

          var conn4 = hub.getDataConn("qux", SyncDataConn.class);
          assertThat(conn4).isNotNull();

          ///

          conn1 = hub.getDataConn("foo", AsyncDataConn.class);
          assertThat(conn1).isNotNull();

          conn2 = hub.getDataConn("bar", SyncDataConn.class);
          assertThat(conn2).isNotNull();

          conn3 = hub.getDataConn("baz", AsyncDataConn.class);
          assertThat(conn3).isNotNull();

          conn4 = hub.getDataConn("qux", SyncDataConn.class);
          assertThat(conn4).isNotNull();

          hub.commit();
        } catch (Exception e) {
          fail(e);
        }

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 setupped",
              "AsyncDataSrc 3 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 created DataConn",
              "AsyncDataSrc 3 created DataConn",
              "SyncDataSrc 4 created DataConn",
              "AsyncDataConn 1 pre committed",
              "SyncDataConn 2 pre committed",
              "AsyncDataConn 3 pre committed",
              "SyncDataConn 4 pre committed",
              "AsyncDataConn 1 committed",
              "SyncDataConn 2 committed",
              "AsyncDataConn 3 committed",
              "SyncDataConn 4 committed",
              "SyncDataConn 4 closed",
              "AsyncDataConn 3 closed",
              "SyncDataConn 2 closed",
              "AsyncDataConn 1 closed",
              "SyncDataSrc 4 closed",
              "AsyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void fail_to_cast_new_data_conn() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("bar", new SyncDataSrc(2, FAIL__NOT, logger));

        try {
          hub.begin();
        } catch (Exception e) {
          fail(e);
        }

        try {
          hub.getDataConn("foo", SyncDataConn.class);
          fail();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToCastDataConn rsn -> {
              assertThat(rsn.name()).isEqualTo("foo");
              assertThat(rsn.castToType())
                  .isEqualTo("com.github.sttk.sabi.internal.DataHubInnerTest$SyncDataConn");
            }
            default -> fail(e);
          }
        } catch (Exception e) {
          fail(e);
        }

        try {
          hub.getDataConn("bar", AsyncDataConn.class);
          fail();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToCastDataConn rsn -> {
              assertThat(rsn.name()).isEqualTo("bar");
              assertThat(rsn.castToType())
                  .isEqualTo("com.github.sttk.sabi.internal.DataHubInnerTest$AsyncDataConn");
            }
            default -> fail(e);
          }
        } catch (Exception e) {
          fail(e);
        }

        hub.end();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 created DataConn",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void fail_to_cast_reused_data_conn() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("bar", new SyncDataSrc(2, FAIL__NOT, logger));

        try {
          hub.begin();
        } catch (Exception e) {
          fail(e);
        }

        var conn1 = hub.getDataConn("foo", AsyncDataConn.class);
        assertThat(conn1).isInstanceOf(AsyncDataConn.class);

        var conn2 = hub.getDataConn("bar", SyncDataConn.class);
        assertThat(conn2).isInstanceOf(SyncDataConn.class);

        try {
          hub.getDataConn("foo", SyncDataConn.class);
          fail();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToCastDataConn rsn -> {
              assertThat(rsn.name()).isEqualTo("foo");
              assertThat(rsn.castToType())
                  .isEqualTo("com.github.sttk.sabi.internal.DataHubInnerTest$SyncDataConn");
            }
            default -> fail(e);
          }
        } catch (Exception e) {
          fail(e);
        }

        try {
          hub.getDataConn("bar", AsyncDataConn.class);
          fail();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToCastDataConn rsn -> {
              assertThat(rsn.name()).isEqualTo("bar");
              assertThat(rsn.castToType())
                  .isEqualTo("com.github.sttk.sabi.internal.DataHubInnerTest$AsyncDataConn");
            }
            default -> fail(e);
          }
        } catch (Exception e) {
          fail(e);
        }

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 created DataConn",
              "SyncDataConn 2 closed",
              "AsyncDataConn 1 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void fail_to_create_data_conn() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("bar", new SyncDataSrc(2, FAIL__CREATE_DATA_CONN, logger));

        try {
          hub.begin();
        } catch (Exception e) {
          fail(e);
        }

        var conn1 = hub.getDataConn("foo", AsyncDataConn.class);
        assertThat(conn1).isInstanceOf(AsyncDataConn.class);

        try {
          hub.getDataConn("bar", AsyncDataConn.class);
          fail();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToCreateDataConn rsn -> {
              assertThat(rsn.name()).isEqualTo("bar");
              assertThat(rsn.dataConnType())
                  .isEqualTo("com.github.sttk.sabi.internal.DataHubInnerTest$AsyncDataConn");
            }
            default -> fail(e);
          }
        } catch (Exception e) {
          fail(e);
        }

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 failed to create a DataConn",
              "AsyncDataConn 1 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void fail_to_create_data_conn_because_of_no_data_src() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("bar", new SyncDataSrc(2, FAIL__NOT, logger));

        try {
          hub.begin();
        } catch (Exception e) {
          fail(e);
        }

        try {
          hub.getDataConn("baz", SyncDataConn.class);
          fail();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.NoDataSrcToCreateDataConn rsn -> {
              assertThat(rsn.name()).isEqualTo("baz");
              assertThat(rsn.dataConnType())
                  .isEqualTo("com.github.sttk.sabi.internal.DataHubInnerTest$SyncDataConn");
            }
            default -> fail(e);
          }
        } catch (Exception e) {
          fail(e);
        }

        try {
          hub.getDataConn("qux", AsyncDataConn.class);
          fail();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.NoDataSrcToCreateDataConn rsn -> {
              assertThat(rsn.name()).isEqualTo("qux");
              assertThat(rsn.dataConnType())
                  .isEqualTo("com.github.sttk.sabi.internal.DataHubInnerTest$AsyncDataConn");
            }
            default -> fail(e);
          }
        } catch (Exception e) {
          fail(e);
        }

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 2 setupped",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void commit_when_no_data_conn() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__NOT, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__NOT, logger));

        try {
          hub.begin();
          hub.commit();
          hub.end();
        } catch (Exception e) {
          fail(e);
        }

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 setupped",
              "AsyncDataSrc 3 setupped",
              "SyncDataSrc 4 closed",
              "AsyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void commit_but_fail_global_sync() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__COMMIT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__NOT, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__NOT, logger));

        hub.begin();

        var conn1 = hub.getDataConn("foo", AsyncDataConn.class);
        assertThat(conn1).isNotNull();

        var conn2 = hub.getDataConn("bar", SyncDataConn.class);
        assertThat(conn2).isNotNull();

        var conn3 = hub.getDataConn("baz", AsyncDataConn.class);
        assertThat(conn3).isNotNull();

        var conn4 = hub.getDataConn("qux", SyncDataConn.class);
        assertThat(conn4).isNotNull();

        try {
          hub.commit();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToCommitDataConn rsn -> {
              assertThat(rsn.errors()).hasSize(1);
              var e2 = rsn.errors().get("bar");
              assertThat(e2.getReason()).isEqualTo("ZZZ");
            }
            default -> fail();
          }
        } catch (Exception e) {
          fail(e);
        }

        hub.end();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 setupped",
              "AsyncDataSrc 3 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 created DataConn",
              "AsyncDataSrc 3 created DataConn",
              "SyncDataSrc 4 created DataConn",
              "AsyncDataConn 1 pre committed",
              "SyncDataConn 2 pre committed",
              "AsyncDataConn 3 pre committed",
              "SyncDataConn 4 pre committed",
              "AsyncDataConn 1 committed",
              "SyncDataConn 2 failed to commit",
              "SyncDataConn 4 closed",
              "AsyncDataConn 3 closed",
              "SyncDataConn 2 closed",
              "AsyncDataConn 1 closed",
              "SyncDataSrc 4 closed",
              "AsyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void commit_but_fail_global_async() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__COMMIT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__NOT, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__NOT, logger));

        hub.begin();

        var conn1 = hub.getDataConn("foo", AsyncDataConn.class);
        assertThat(conn1).isNotNull();

        var conn2 = hub.getDataConn("bar", SyncDataConn.class);
        assertThat(conn2).isNotNull();

        var conn3 = hub.getDataConn("baz", AsyncDataConn.class);
        assertThat(conn3).isNotNull();

        var conn4 = hub.getDataConn("qux", SyncDataConn.class);
        assertThat(conn4).isNotNull();

        try {
          hub.commit();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToCommitDataConn rsn -> {
              assertThat(rsn.errors()).hasSize(1);
              var e2 = rsn.errors().get("foo");
              assertThat(e2.getReason()).isEqualTo("VVV");
            }
            default -> fail();
          }
        } catch (Exception e) {
          fail(e);
        }

        hub.end();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 setupped",
              "AsyncDataSrc 3 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 created DataConn",
              "AsyncDataSrc 3 created DataConn",
              "SyncDataSrc 4 created DataConn",
              "AsyncDataConn 1 pre committed",
              "SyncDataConn 2 pre committed",
              "AsyncDataConn 3 pre committed",
              "SyncDataConn 4 pre committed",
              "AsyncDataConn 1 failed to commit",
              "SyncDataConn 4 closed",
              "AsyncDataConn 3 closed",
              "SyncDataConn 2 closed",
              "AsyncDataConn 1 closed",
              "SyncDataSrc 4 closed",
              "AsyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void commit_but_fail_local_sync() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__NOT, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__COMMIT, logger));

        hub.begin();

        var conn1 = hub.getDataConn("foo", AsyncDataConn.class);
        assertThat(conn1).isNotNull();

        var conn2 = hub.getDataConn("bar", SyncDataConn.class);
        assertThat(conn2).isNotNull();

        var conn3 = hub.getDataConn("baz", AsyncDataConn.class);
        assertThat(conn3).isNotNull();

        var conn4 = hub.getDataConn("qux", SyncDataConn.class);
        assertThat(conn4).isNotNull();

        try {
          hub.commit();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToCommitDataConn rsn -> {
              assertThat(rsn.errors()).hasSize(1);
              var e2 = rsn.errors().get("qux");
              assertThat(e2.getReason()).isEqualTo("ZZZ");
            }
            default -> fail();
          }
        } catch (Exception e) {
          fail(e);
        }

        hub.end();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 setupped",
              "AsyncDataSrc 3 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 created DataConn",
              "AsyncDataSrc 3 created DataConn",
              "SyncDataSrc 4 created DataConn",
              "AsyncDataConn 1 pre committed",
              "SyncDataConn 2 pre committed",
              "AsyncDataConn 3 pre committed",
              "SyncDataConn 4 pre committed",
              "AsyncDataConn 1 committed",
              "SyncDataConn 2 committed",
              "AsyncDataConn 3 committed",
              "SyncDataConn 4 failed to commit",
              "SyncDataConn 4 closed",
              "AsyncDataConn 3 closed",
              "SyncDataConn 2 closed",
              "AsyncDataConn 1 closed",
              "SyncDataSrc 4 closed",
              "AsyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void commit_but_fail_local_async() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__COMMIT, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__NOT, logger));

        hub.begin();

        var conn1 = hub.getDataConn("foo", AsyncDataConn.class);
        assertThat(conn1).isNotNull();

        var conn2 = hub.getDataConn("bar", SyncDataConn.class);
        assertThat(conn2).isNotNull();

        var conn3 = hub.getDataConn("baz", AsyncDataConn.class);
        assertThat(conn3).isNotNull();

        var conn4 = hub.getDataConn("qux", SyncDataConn.class);
        assertThat(conn4).isNotNull();

        try {
          hub.commit();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToCommitDataConn rsn -> {
              assertThat(rsn.errors()).hasSize(1);
              var e2 = rsn.errors().get("baz");
              assertThat(e2.getReason()).isEqualTo("VVV");
            }
            default -> fail();
          }
        } catch (Exception e) {
          fail(e);
        }

        hub.end();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 setupped",
              "AsyncDataSrc 3 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 created DataConn",
              "AsyncDataSrc 3 created DataConn",
              "SyncDataSrc 4 created DataConn",
              "AsyncDataConn 1 pre committed",
              "SyncDataConn 2 pre committed",
              "AsyncDataConn 3 pre committed",
              "SyncDataConn 4 pre committed",
              "AsyncDataConn 1 committed",
              "SyncDataConn 2 committed",
              "AsyncDataConn 3 failed to commit",
              "SyncDataConn 4 closed",
              "AsyncDataConn 3 closed",
              "SyncDataConn 2 closed",
              "AsyncDataConn 1 closed",
              "SyncDataSrc 4 closed",
              "AsyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void pre_commit_but_fail_global_sync() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__PRE_COMMIT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__NOT, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__NOT, logger));

        hub.begin();

        var conn1 = hub.getDataConn("foo", AsyncDataConn.class);
        assertThat(conn1).isNotNull();

        var conn2 = hub.getDataConn("bar", SyncDataConn.class);
        assertThat(conn2).isNotNull();

        var conn3 = hub.getDataConn("baz", AsyncDataConn.class);
        assertThat(conn3).isNotNull();

        var conn4 = hub.getDataConn("qux", SyncDataConn.class);
        assertThat(conn4).isNotNull();

        try {
          hub.commit();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToPreCommitDataConn rsn -> {
              assertThat(rsn.errors()).hasSize(1);
              var e2 = rsn.errors().get("bar");
              assertThat(e2.getReason()).isEqualTo("zzz");
            }
            default -> fail();
          }
        } catch (Exception e) {
          fail(e);
        }

        hub.end();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 setupped",
              "AsyncDataSrc 3 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 created DataConn",
              "AsyncDataSrc 3 created DataConn",
              "SyncDataSrc 4 created DataConn",
              "AsyncDataConn 1 pre committed",
              "SyncDataConn 2 failed to pre commit",
              "SyncDataConn 4 closed",
              "AsyncDataConn 3 closed",
              "SyncDataConn 2 closed",
              "AsyncDataConn 1 closed",
              "SyncDataSrc 4 closed",
              "AsyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void pre_commit_but_fail_global_async() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__PRE_COMMIT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__NOT, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__NOT, logger));

        hub.begin();

        var conn1 = hub.getDataConn("foo", AsyncDataConn.class);
        assertThat(conn1).isNotNull();

        var conn2 = hub.getDataConn("bar", SyncDataConn.class);
        assertThat(conn2).isNotNull();

        var conn3 = hub.getDataConn("baz", AsyncDataConn.class);
        assertThat(conn3).isNotNull();

        var conn4 = hub.getDataConn("qux", SyncDataConn.class);
        assertThat(conn4).isNotNull();

        try {
          hub.commit();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToPreCommitDataConn rsn -> {
              assertThat(rsn.errors()).hasSize(1);
              var e2 = rsn.errors().get("bar");
              assertThat(e2.getReason()).isEqualTo("zzz");
            }
            default -> fail();
          }
        } catch (Exception e) {
          fail(e);
        }

        hub.end();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 setupped",
              "AsyncDataSrc 3 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 created DataConn",
              "AsyncDataSrc 3 created DataConn",
              "SyncDataSrc 4 created DataConn",
              "AsyncDataConn 1 pre committed",
              "SyncDataConn 2 failed to pre commit",
              "SyncDataConn 4 closed",
              "AsyncDataConn 3 closed",
              "SyncDataConn 2 closed",
              "AsyncDataConn 1 closed",
              "SyncDataSrc 4 closed",
              "AsyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void pre_commit_but_fail_local_sync() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__NOT, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__PRE_COMMIT, logger));

        hub.begin();

        var conn1 = hub.getDataConn("foo", AsyncDataConn.class);
        assertThat(conn1).isNotNull();

        var conn2 = hub.getDataConn("bar", SyncDataConn.class);
        assertThat(conn2).isNotNull();

        var conn3 = hub.getDataConn("baz", AsyncDataConn.class);
        assertThat(conn3).isNotNull();

        var conn4 = hub.getDataConn("qux", SyncDataConn.class);
        assertThat(conn4).isNotNull();

        try {
          hub.commit();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToPreCommitDataConn rsn -> {
              assertThat(rsn.errors()).hasSize(1);
              var e2 = rsn.errors().get("qux");
              assertThat(e2.getReason()).isEqualTo("zzz");
            }
            default -> fail();
          }
        } catch (Exception e) {
          fail(e);
        }

        hub.end();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 setupped",
              "AsyncDataSrc 3 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 created DataConn",
              "AsyncDataSrc 3 created DataConn",
              "SyncDataSrc 4 created DataConn",
              "AsyncDataConn 1 pre committed",
              "SyncDataConn 2 pre committed",
              "AsyncDataConn 3 pre committed",
              "SyncDataConn 4 failed to pre commit",
              "SyncDataConn 4 closed",
              "AsyncDataConn 3 closed",
              "SyncDataConn 2 closed",
              "AsyncDataConn 1 closed",
              "SyncDataSrc 4 closed",
              "AsyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void pre_commit_but_fail_local_async() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__PRE_COMMIT, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__NOT, logger));

        hub.begin();

        var conn1 = hub.getDataConn("foo", AsyncDataConn.class);
        assertThat(conn1).isNotNull();

        var conn2 = hub.getDataConn("bar", SyncDataConn.class);
        assertThat(conn2).isNotNull();

        var conn3 = hub.getDataConn("baz", AsyncDataConn.class);
        assertThat(conn3).isNotNull();

        var conn4 = hub.getDataConn("qux", SyncDataConn.class);
        assertThat(conn4).isNotNull();

        try {
          hub.commit();
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToPreCommitDataConn rsn -> {
              assertThat(rsn.errors()).hasSize(1);
              var e2 = rsn.errors().get("baz");
              assertThat(e2.getReason()).isEqualTo("vvv");
            }
            default -> fail();
          }
        } catch (Exception e) {
          fail(e);
        }

        hub.end();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 setupped",
              "AsyncDataSrc 3 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 created DataConn",
              "AsyncDataSrc 3 created DataConn",
              "SyncDataSrc 4 created DataConn",
              "AsyncDataConn 1 pre committed",
              "SyncDataConn 2 pre committed",
              "AsyncDataConn 3 failed to pre commit",
              "SyncDataConn 4 closed",
              "AsyncDataConn 3 closed",
              "SyncDataConn 2 closed",
              "AsyncDataConn 1 closed",
              "SyncDataSrc 4 closed",
              "AsyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void rollback() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__NOT, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__NOT, logger));

        hub.begin();

        var conn1 = hub.getDataConn("foo", AsyncDataConn.class);
        assertThat(conn1).isNotNull();

        var conn2 = hub.getDataConn("bar", SyncDataConn.class);
        assertThat(conn2).isNotNull();

        var conn3 = hub.getDataConn("baz", AsyncDataConn.class);
        assertThat(conn3).isNotNull();

        var conn4 = hub.getDataConn("qux", SyncDataConn.class);
        assertThat(conn4).isNotNull();

        hub.rollback();
        hub.end();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 setupped",
              "AsyncDataSrc 3 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 created DataConn",
              "AsyncDataSrc 3 created DataConn",
              "SyncDataSrc 4 created DataConn",
              "AsyncDataConn 1 rollbacked",
              "SyncDataConn 2 rollbacked",
              "AsyncDataConn 3 rollbacked",
              "SyncDataConn 4 rollbacked",
              "SyncDataConn 4 closed",
              "AsyncDataConn 3 closed",
              "SyncDataConn 2 closed",
              "AsyncDataConn 1 closed",
              "SyncDataSrc 4 closed",
              "AsyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void force_back() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__NOT, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__NOT, logger));

        hub.begin();

        var conn1 = hub.getDataConn("foo", AsyncDataConn.class);
        assertThat(conn1).isNotNull();

        var conn2 = hub.getDataConn("bar", SyncDataConn.class);
        assertThat(conn2).isNotNull();

        var conn3 = hub.getDataConn("baz", AsyncDataConn.class);
        assertThat(conn3).isNotNull();

        var conn4 = hub.getDataConn("qux", SyncDataConn.class);
        assertThat(conn4).isNotNull();

        hub.commit();
        hub.rollback();
        hub.end();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 setupped",
              "AsyncDataSrc 3 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 created DataConn",
              "AsyncDataSrc 3 created DataConn",
              "SyncDataSrc 4 created DataConn",
              "AsyncDataConn 1 pre committed",
              "SyncDataConn 2 pre committed",
              "AsyncDataConn 3 pre committed",
              "SyncDataConn 4 pre committed",
              "AsyncDataConn 1 committed",
              "SyncDataConn 2 committed",
              "AsyncDataConn 3 committed",
              "SyncDataConn 4 committed",
              "AsyncDataConn 1 forced back",
              "SyncDataConn 2 forced back",
              "AsyncDataConn 3 forced back",
              "SyncDataConn 4 forced back",
              "SyncDataConn 4 closed",
              "AsyncDataConn 3 closed",
              "SyncDataConn 2 closed",
              "AsyncDataConn 1 closed",
              "SyncDataSrc 4 closed",
              "AsyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }

    @Test
    void post_commit() {
      var logger = new ArrayList<String>();

      DataHubInner.usesGlobal("foo", new AsyncDataSrc(1, FAIL__NOT, logger));
      DataHubInner.usesGlobal("bar", new SyncDataSrc(2, FAIL__NOT, logger));

      try (var ac = DataHubInner.setupGlobals()) {
        suppressWarnings_unused(ac);
        var hub = new DataHubInner();

        hub.uses("baz", new AsyncDataSrc(3, FAIL__NOT, logger));
        hub.uses("qux", new SyncDataSrc(4, FAIL__NOT, logger));

        hub.begin();

        var conn1 = hub.getDataConn("foo", AsyncDataConn.class);
        assertThat(conn1).isNotNull();

        var conn2 = hub.getDataConn("bar", SyncDataConn.class);
        assertThat(conn2).isNotNull();

        var conn3 = hub.getDataConn("baz", AsyncDataConn.class);
        assertThat(conn3).isNotNull();

        var conn4 = hub.getDataConn("qux", SyncDataConn.class);
        assertThat(conn4).isNotNull();

        hub.postCommit();
        hub.end();

        hub.close();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc 2 setupped",
              "AsyncDataSrc 1 setupped",
              "SyncDataSrc 4 setupped",
              "AsyncDataSrc 3 setupped",
              "AsyncDataSrc 1 created DataConn",
              "SyncDataSrc 2 created DataConn",
              "AsyncDataSrc 3 created DataConn",
              "SyncDataSrc 4 created DataConn",
              "AsyncDataConn 1 post committed",
              "SyncDataConn 2 post committed",
              "AsyncDataConn 3 post committed",
              "SyncDataConn 4 post committed",
              "SyncDataConn 4 closed",
              "AsyncDataConn 3 closed",
              "SyncDataConn 2 closed",
              "AsyncDataConn 1 closed",
              "SyncDataSrc 4 closed",
              "AsyncDataSrc 3 closed",
              "SyncDataSrc 2 closed",
              "AsyncDataSrc 1 closed");
    }
  }
}
