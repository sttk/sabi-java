package com.github.sttk.sabi;

import static com.github.sttk.sabi.TestDoubles.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import java.util.ArrayList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class LogicRunnerTest {
  private LogicRunnerTest() {}

  @Nested
  class TestNewUsesDisuses {
    @Test
    void testNew() {
      var hub = new DataHub();
      try (var runner = new LogicRunner(hub)) {
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();
      }
    }

    @Test
    void testUses() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        try {
          runner.hub.run.begin();
        } catch (Exception e) {
          fail(e);
        }

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }
    }

    @Test
    void testUsesButAlreadyFixed() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(1);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        try {
          runner.hub.run.begin();
        } catch (Exception e) {
          fail(e);
        }

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(1);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(1);
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();

        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(1);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(1);
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }
    }

    @Test
    void testDisesAndOk() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.disuses("foo");

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(1);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.disuses("bar");

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(0);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();
      }
    }

    @Test
    void testDisusesAndFix() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.disuses("foo");

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(1);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.disuses("bar");

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(0);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        try {
          runner.hub.run.begin();
        } catch (Exception e) {
          fail(e);
        }

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();

        runner.uses("baz", new SyncDataSrc(3, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();

        runner.disuses("bar");

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();

        runner.hub.run.end();

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.disuses("bar");

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(1);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(1);
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.disuses("foo");

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(0);
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();
      }
    }
  }

  @Nested
  class TestRun {
    @Test
    void testRunWithNoDataSrc() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.run(
            data -> {
              logger.add("Run the logic");
            });

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();
      } catch (Err e) {
        fail(e);
      }

      assertThat(logger).containsExactly("Run the logic");
    }

    @Test
    void testRunWithSomeDataSrc() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.run(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();
      } catch (Err err) {
        fail(err);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testRunButFailedToBegin() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.Setup));
        runner.uses("baz", new SyncDataSrc(3, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(3);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.run(
            data -> {
              logger.add("Run the logic");
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case DataHubError.FailToSetupLocalDataSrcs r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index()).isEqualTo(1);
            assertThat(r.errors().get(0).name()).isEqualTo("bar");
            assertThat(r.errors().get(0).err().getReason()).isEqualTo("XXX");
          }
          default -> fail(err);
        }
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#new 3",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2 failed",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testRunButFailedToRunLogic() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        runner.run(
            data -> {
              logger.add("Run the logic but failed");
              throw new Err("logic error");
            });
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case String s -> {
            assertThat(s).isEqualTo("logic error");
          }
          default -> fail(err);
        }
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "Run the logic but failed",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testRunIfAbnormal() {
      try (var runner = new LogicRunner(new DataHub())) {
        runner.hub.run.localDataSrcManager.isAbnormal = true;
        runner.run(data -> {});
      } catch (Err err) {
        switch (err.getReason()) {
          case DataHubError.AbnormalState r -> {}
          default -> fail(err);
        }
      }
    }
  }

  @Nested
  class TestStart {
    @Test
    void testStartWithNoDataSrc() {
      try (var runner = new LogicRunner(new DataHub())) {
        var r = runner.start();
        r.end();
      } catch (Err err) {
        fail(err);
      }
    }

    @Test
    void testStartWithSomeDataSrcs() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        var r = runner.start();
        r.end();
      } catch (Err err) {
        fail(err);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testStartButFailedToBegin() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.Setup));
        runner.uses("baz", new SyncDataSrc(3, logger, Fail.None));

        runner.start().end();
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case DataHubError.FailToSetupLocalDataSrcs r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index()).isEqualTo(1);
            assertThat(r.errors().get(0).name()).isEqualTo("bar");
            assertThat(r.errors().get(0).err().getReason()).isEqualTo("XXX");
          }
          default -> fail(err);
        }
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#new 3",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2 failed",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }
  }

  @Nested
  class TestTxn {
    @Test
    void testTxnWithNoDataSrc() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.txn(
            data -> {
              logger.add("Run the logic");
            });

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();
      } catch (Err err) {
        fail(err);
      }

      assertThat(logger).containsExactly("Run the logic");
    }

    @Test
    void testTxnWithSomeDataSrcs() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();
      } catch (Err err) {
        fail(err);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#preCommit 1",
              "SyncDataConn#preCommit 2",
              "SyncDataConn#commit 1",
              "SyncDataConn#commit 2",
              "SyncDataConn#postCommit 1",
              "SyncDataConn#postCommit 2",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testTxnButFailedToBegin() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.Setup));
        runner.uses("baz", new SyncDataSrc(3, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(3);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case DataHubError.FailToSetupLocalDataSrcs r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index()).isEqualTo(1);
            assertThat(r.errors().get(0).name()).isEqualTo("bar");
            assertThat(r.errors().get(0).err().getReason()).isEqualTo("XXX");
          }
          default -> fail(err);
        }
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#new 3",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2 failed",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testTxnButFailedToRunLogic() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic but failed");
              throw new Err("logic error");
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case String s -> assertThat(s).isEqualTo("logic error");
          default -> fail(err);
        }
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic but failed",
              "SyncDataConn#rollback 1",
              "SyncDataConn#rollback 2",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String logic error, file = LogicRunnerTest.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String logic error, file = LogicRunnerTest.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String logic error, file = LogicRunnerTest.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String logic error, file = LogicRunnerTest.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testTxnButFailedToPreCommit() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.PreCommit));
        runner.uses("baz", new SyncDataSrc(3, logger, Fail.None));

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              var conn3 = data.getDataConn("baz", SyncDataConn.class);
              logger.add("Run the logic");
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case DataConnError.FailToPreCommitDataConn r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index()).isEqualTo(1);
            assertThat(r.errors().get(0).name()).isEqualTo("bar");
            assertThat(r.errors().get(0).err().getReason()).isEqualTo("zzz");
          }
          default -> fail(err);
        }
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#new 3",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#setup 3",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "SyncDataSrc#createDataConn 3",
              "SyncDataConn#new 3",
              "Run the logic",
              "SyncDataConn#preCommit 1",
              "SyncDataConn#preCommit 2 failed",
              "SyncDataConn#rollback 1",
              "SyncDataConn#rollback 2",
              "SyncDataConn#rollback 3",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestDoubles.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestDoubles.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 3",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestDoubles.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 3",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 3",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testTxnButFailedToCommit() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.Commit));
        runner.uses("baz", new SyncDataSrc(3, logger, Fail.None));

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              var conn3 = data.getDataConn("baz", SyncDataConn.class);
              logger.add("Run the logic");
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case DataConnError.FailToCommitDataConn r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index()).isEqualTo(1);
            assertThat(r.errors().get(0).name()).isEqualTo("bar");
            assertThat(r.errors().get(0).err().getReason()).isEqualTo("ZZZ");
          }
          default -> fail(err);
        }
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#new 3",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#setup 3",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "SyncDataSrc#createDataConn 3",
              "SyncDataConn#new 3",
              "Run the logic",
              "SyncDataConn#preCommit 1",
              "SyncDataConn#preCommit 2",
              "SyncDataConn#preCommit 3",
              "SyncDataConn#commit 1",
              "SyncDataConn#commit 2 failed",
              "SyncDataConn#rollback 2",
              "SyncDataConn#rollback 3",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestDoubles.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestDoubles.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 3",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestDoubles.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 3",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 3",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testTxnButFailedToPostCommit() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.PostCommit));
        runner.uses("baz", new SyncDataSrc(3, logger, Fail.None));

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              var conn3 = data.getDataConn("baz", SyncDataConn.class);
              logger.add("Run the logic");
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case DataConnError.FailToPostCommitDataConn r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index()).isEqualTo(1);
            assertThat(r.errors().get(0).name()).isEqualTo("bar");
            assertThat(r.errors().get(0).err().getReason()).isEqualTo("!!!");
          }
          default -> fail(err);
        }
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#new 3",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#setup 3",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "SyncDataSrc#createDataConn 3",
              "SyncDataConn#new 3",
              "Run the logic",
              "SyncDataConn#preCommit 1",
              "SyncDataConn#preCommit 2",
              "SyncDataConn#preCommit 3",
              "SyncDataConn#commit 1",
              "SyncDataConn#commit 2",
              "SyncDataConn#commit 3",
              "SyncDataConn#postCommit 1",
              "SyncDataConn#postCommit 2 failed",
              "SyncDataConn#postCommit 3",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = *** }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = *** }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 3",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = *** }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]",
              "SyncDataConn#close 3",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 3",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testTxnButFailedToRollback() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.Rollback));
        runner.uses("baz", new SyncDataSrc(3, logger, Fail.None));

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              var conn3 = data.getDataConn("baz", SyncDataConn.class);
              logger.add("Run the logic but failed");
              throw new Err("fail");
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case String s -> assertThat(s).isEqualTo("fail");
          default -> fail(err);
        }
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#new 3",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#setup 3",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "SyncDataSrc#createDataConn 3",
              "SyncDataConn#new 3",
              "Run the logic but failed",
              "SyncDataConn#rollback 1",
              "SyncDataConn#rollback 2 failed",
              "SyncDataConn#rollback 3",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestDoubles.java, line = *** }}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestDoubles.java, line = *** }}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 3",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestDoubles.java, line = *** }}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 3",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 3",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testTxnWithCommitOrder() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub("bar", "foo"))) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.indexMap).hasSize(2);
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.list).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.indexMap).hasSize(2);
        assertThat(runner.hub.run.fixed).isFalse();
      } catch (Err err) {
        fail(err);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#preCommit 2",
              "SyncDataConn#preCommit 1",
              "SyncDataConn#commit 2",
              "SyncDataConn#commit 1",
              "SyncDataConn#postCommit 2",
              "SyncDataConn#postCommit 1",
              "SyncDataConn#close 1",
              "SyncDataConn#close 2",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testRunIfIsAbnormal() {
      try (var runner = new LogicRunner(new DataHub())) {
        runner.hub.run.localDataSrcManager.isAbnormal = true;
        runner.txn(data -> {});
      } catch (Err err) {
        switch (err.getReason()) {
          case DataHubError.AbnormalState r -> {}
          default -> fail(err);
        }
      }
    }
  }

  @Nested
  class TestBeginTxn {
    @Test
    void testBeginTxnWithNoDataSrc() {
      try (var runner = new LogicRunner(new DataHub())) {
        var txn = runner.beginTxn();
        txn.endTxn();
      } catch (Err err) {
        fail(err);
      }
    }

    @Test
    void testBeginTxnWithSomeDataSrcs() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        var txn = runner.beginTxn();
        txn.endTxn();
      } catch (Err err) {
        fail(err);
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testBeginTxnButFailedToBegin() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.Setup));
        runner.uses("baz", new SyncDataSrc(3, logger, Fail.None));

        runner.beginTxn().endTxn();
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case DataHubError.FailToSetupLocalDataSrcs r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index()).isEqualTo(1);
            assertThat(r.errors().get(0).name()).isEqualTo("bar");
            assertThat(r.errors().get(0).err().getReason()).isEqualTo("XXX");
          }
          default -> fail(err);
        }
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#new 3",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2 failed",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }
  }

  @Nested
  class TestFatal {
    @Test
    void testRunLogicError() {
      var logger = new ArrayList<String>();

      LogicRunner runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.run(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic but Error");
              throw new Error("error");
            });
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("error");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic but Error",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testRunLogicRuntimeException() {
      var logger = new ArrayList<String>();

      LogicRunner runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.run(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic but RuntimeException");
              throw new RuntimeException("error");
            });
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("error");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic but RuntimeException",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testRunCloseError() {
      var logger = new ArrayList<String>();

      LogicRunner runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.CloseError));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.run(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("close Error");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#close 2 Error",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testRunCloseRuntimeException() {
      var logger = new ArrayList<String>();

      LogicRunner runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.CloseRuntimeException));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.run(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("close RuntimeException");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#close 2 RuntimeException",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testTxnLogicError() {
      var logger = new ArrayList<String>();

      LogicRunner runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic but Error");
              throw new Error("error");
            });
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("error");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic but Error",
              "SyncDataConn#rollback 1",
              "SyncDataConn#rollback 2",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicRunner.java, line = ***, cause = java.lang.Error: error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicRunner.java, line = ***, cause = java.lang.Error: error }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicRunner.java, line = ***, cause = java.lang.Error: error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicRunner.java, line = ***, cause = java.lang.Error: error }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testTxnLogicRuntimeException() {
      var logger = new ArrayList<String>();

      LogicRunner runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic but RuntimeException");
              throw new RuntimeException("error");
            });
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("error");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic but RuntimeException",
              "SyncDataConn#rollback 1",
              "SyncDataConn#rollback 2",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicRunner.java, line = ***, cause = java.lang.RuntimeException: error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicRunner.java, line = ***, cause = java.lang.RuntimeException: error }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicRunner.java, line = ***, cause = java.lang.RuntimeException: error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicRunner.java, line = ***, cause = java.lang.RuntimeException: error }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testPreCommitError() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.PreCommitError));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("preCommit Error");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#preCommit 1 Error",
              "SyncDataConn#rollback 1",
              "SyncDataConn#rollback 2",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.Error: preCommit Error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.Error: preCommit Error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testPreCommitRuntimeException() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.PreCommitRuntimeException));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("preCommit RuntimeException");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#preCommit 1",
              "SyncDataConn#preCommit 2 RuntimeException",
              "SyncDataConn#rollback 1",
              "SyncDataConn#rollback 2",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: preCommit RuntimeException }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: preCommit RuntimeException }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testCommitError() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.CommitError));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("commit Error");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#preCommit 1",
              "SyncDataConn#preCommit 2",
              "SyncDataConn#commit 1 Error",
              "SyncDataConn#rollback 1",
              "SyncDataConn#rollback 2",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.Error: commit Error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.Error: commit Error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testCommitRuntimeException() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.CommitRuntimeException));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("commit RuntimeException");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#preCommit 1",
              "SyncDataConn#preCommit 2",
              "SyncDataConn#commit 1 RuntimeException",
              "SyncDataConn#rollback 1",
              "SyncDataConn#rollback 2",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: commit RuntimeException }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: commit RuntimeException }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testPostCommitError() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.PostCommitError));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("postCommit Error");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#preCommit 1",
              "SyncDataConn#preCommit 2",
              "SyncDataConn#commit 1",
              "SyncDataConn#commit 2",
              "SyncDataConn#postCommit 1 Error",
              "SyncDataConn#postCommit 2",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.Error: postCommit Error }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.Error: postCommit Error }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testPostCommitRuntimeException() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.PostCommitRuntimeException));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("postCommit RuntimeException");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#preCommit 1",
              "SyncDataConn#preCommit 2",
              "SyncDataConn#commit 1",
              "SyncDataConn#commit 2",
              "SyncDataConn#postCommit 1",
              "SyncDataConn#postCommit 2 RuntimeException",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: postCommit RuntimeException }} rollback:{State:NoneByNotRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: postCommit RuntimeException }} rollback:{State:NoneByNotRolledBack Err:null}}]",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testRollbackError() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.RollbackError));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.RollbackError));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
              throw new Err("run logic error");
            });
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("rollback Error");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#rollback 1 Error",
              "SyncDataConn#rollback 2 Error",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String run logic error, file = LogicRunnerTest.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.Error: rollback Error }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String run logic error, file = LogicRunnerTest.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.Error: rollback Error }}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String run logic error, file = LogicRunnerTest.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.Error: rollback Error }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String run logic error, file = LogicRunnerTest.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.Error: rollback Error }}}]",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testRollbackRuntimeException() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.RollbackRuntimeException));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.RollbackRuntimeException));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
              throw new Err("run logic error");
            });
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("rollback RuntimeException");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#rollback 1 RuntimeException",
              "SyncDataConn#rollback 2 RuntimeException",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String run logic error, file = LogicRunnerTest.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: rollback RuntimeException }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String run logic error, file = LogicRunnerTest.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: rollback RuntimeException }}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String run logic error, file = LogicRunnerTest.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: rollback RuntimeException }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String run logic error, file = LogicRunnerTest.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: rollback RuntimeException }}}]",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testCloseError() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.CloseError));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.CloseError));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("close Error");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#preCommit 1",
              "SyncDataConn#preCommit 2",
              "SyncDataConn#commit 1",
              "SyncDataConn#commit 2",
              "SyncDataConn#postCommit 1",
              "SyncDataConn#postCommit 2",
              "SyncDataConn#close 2 Error",
              "SyncDataConn#close 1 Error",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testCloseRuntimeException() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.CloseRuntimeException));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.CloseRuntimeException));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner.txn(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("close RuntimeException");
        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isTrue();
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "Run the logic",
              "SyncDataConn#preCommit 1",
              "SyncDataConn#preCommit 2",
              "SyncDataConn#commit 1",
              "SyncDataConn#commit 2",
              "SyncDataConn#postCommit 1",
              "SyncDataConn#postCommit 2",
              "SyncDataConn#close 2 RuntimeException",
              "SyncDataConn#close 1 RuntimeException",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }
  }
}
