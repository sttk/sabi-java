package com.github.sttk.sabi;

import static com.github.sttk.sabi.TestDoubles.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import java.util.ArrayList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class LogicRunTest {
  private LogicRunTest() {}

  @Nested
  class TestRuns {
    @Test
    void testLogicRunAndOk() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        runner
            .start()
            .runOrBlock(
                (DataAcc data) -> {
                  var conn = data.getDataConn("foo", SyncDataConn.class);
                  logger.add("execute logic-0");
                })
            .run(
                (DataAcc data) -> {
                  var conn = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("execute logic-1");
                })
            .runForce(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("execute logic-2");
                })
            .end();
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
              "execute logic-0",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "execute logic-1",
              "execute logic-2",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testLogicRunButFailToRunButRunForceRuns() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        runner
            .start()
            .run(
                (DataAcc data) -> {
                  var conn = data.getDataConn("foo", SyncDataConn.class);
                  logger.add("execute logic-0");
                  throw new Err("logic-0 failed");
                })
            .run(
                (DataAcc data) -> {
                  var conn = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("execute logic-1");
                })
            .runOrBlock(
                (DataAcc data) -> {
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("execute logic-2");
                })
            .runForce(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  logger.add("execute logic-3");
                })
            .end();
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case DataHubError.FailToRunLogic r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index()).isEqualTo(0);
            assertThat(r.errors().get(0).name()).isEqualTo("LogicRun#run(logic-0)");
            assertThat(r.errors().get(0).err().getReason()).isEqualTo("logic-0 failed");
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
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "execute logic-0",
              "execute logic-3",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testRunnerButFailedToRunOrBlockThenSkipEvenRunForce() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        runner
            .start()
            .runOrBlock(
                (DataAcc data) -> {
                  var conn = data.getDataConn("foo", SyncDataConn.class);
                  logger.add("execute logic-0");
                  throw new Err("logic-0 failed");
                })
            .run(
                (DataAcc data) -> {
                  var conn = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("execute logic-1");
                })
            .runForce(
                (DataAcc data) -> {
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("execute logic-2");
                })
            .runOrBlock(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("execute logic-3");
                })
            .end();
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case DataHubError.FailToRunLogic r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index()).isEqualTo(0);
            assertThat(r.errors().get(0).name()).isEqualTo("LogicRun#runOrBlock(logic-0)");
            assertThat(r.errors().get(0).err().getReason()).isEqualTo("logic-0 failed");
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
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "execute logic-0",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testRunnerAndFailedToRunForceThenSkipRunButRunForceRuns() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        runner
            .start()
            .runForce(
                (DataAcc data) -> {
                  var conn = data.getDataConn("foo", SyncDataConn.class);
                  logger.add("execute logic-0");
                  throw new Err("logic-0 failed");
                })
            .run(
                (DataAcc data) -> {
                  var conn = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("execute logic-1");
                })
            .runForce(
                (DataAcc data) -> {
                  var conn2 = data.getDataConn("foo", SyncDataConn.class);
                  logger.add("execute logic-2");
                })
            .runOrBlock(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("execute logic-3");
                })
            .end();
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case DataHubError.FailToRunLogic r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index()).isEqualTo(0);
            assertThat(r.errors().get(0).name()).isEqualTo("LogicRun#runForce(logic-0)");
            assertThat(r.errors().get(0).err().getReason()).isEqualTo("logic-0 failed");
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
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "execute logic-0",
              "execute logic-2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }
  }

  @Nested
  class TestNestedLogicRunningInDataAcc {
    interface HogeData {
      String getText() throws Err;

      void setText(String text) throws Err;
    }

    static Logic<HogeData> hogeLogic =
        data -> {
          var text = data.getText();
          text = text.toUpperCase();
          data.setText(text);
        };

    interface GettingDataAcc extends HogeData, DataAcc {
      @Override
      default String getText() throws Err {
        var conn = getDataConn("foo", SyncDataConn.class);
        return "hello";
      }
    }

    interface SettingDataAcc extends HogeData, DataAcc {
      @Override
      default void setText(String text) throws Err {
        var conn = getDataConn("bar", SyncDataConn.class);
      }
    }

    interface FugaData {
      void processText() throws Err;
    }

    Logic<FugaData> fugaLogic =
        data -> {
          data.processText();
        };

    interface GetSetDataAcc extends FugaData, DataAcc {
      @Override
      default void processText() throws Err {
        start().run(hogeLogic).end();
      }
    }

    class MyDataHub extends DataHub implements GettingDataAcc, SettingDataAcc, GetSetDataAcc {}

    @Test
    void testRun() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new MyDataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        runner.start().run(fugaLogic).end();
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
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void runsIfAbnormal() {
      try (var runner = new LogicRunner(new DataHub())) {
        runner.hub.run.localDataSrcManager.isAbnormal = true;
        runner.start().run(data -> {}).runForce(data -> {}).runOrBlock(data -> {}).end();
      } catch (Err err) {
        switch (err.getReason()) {
          case DataHubError.AbnormalState r -> {}
          default -> fail(err);
        }
      }
    }
  }

  @Nested
  class TestFatal {
    @Test
    void testRunError() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
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

        runner
            .start()
            .run(
                (DataHub data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but Error");
                  throw new Error("run logic Error");
                })
            .end();
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("run logic Error");
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
    void testRunRuntimeException() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
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

        runner
            .start()
            .run(
                (DataHub data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but RuntimeException");
                  throw new RuntimeException("run logic RuntimeException");
                })
            .end();
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("run logic RuntimeException");
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
    void testRunForceError() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
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

        runner
            .start()
            .runForce(
                (DataHub data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but Error");
                  throw new Error("run logic Error");
                })
            .end();
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("run logic Error");
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
    void testRunForceRuntimeException() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
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

        runner
            .start()
            .runForce(
                (DataHub data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but RuntimeException");
                  throw new RuntimeException("run logic RuntimeException");
                })
            .end();
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("run logic RuntimeException");
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
    void testRunOrBlockError() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
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

        runner
            .start()
            .runOrBlock(
                (DataHub data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but Error");
                  throw new Error("run logic Error");
                })
            .end();
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("run logic Error");
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
    void testRunOrBlockRuntimeException() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
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

        runner
            .start()
            .runOrBlock(
                (DataHub data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but RuntimeException");
                  throw new RuntimeException("run logic RuntimeException");
                })
            .end();
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("run logic RuntimeException");
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

        runner
            .start()
            .run(
                (DataHub data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic");
                })
            .end();
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
              "SyncDataConn#close 1 Error",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testCloseException() {
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

        runner
            .start()
            .run(
                (DataHub data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic");
                })
            .end();
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
              "SyncDataConn#close 1 RuntimeException",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }
  }
}
