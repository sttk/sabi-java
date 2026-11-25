package com.github.sttk.sabi;

import static com.github.sttk.sabi.TestDoubles.*;
import static com.github.sttk.sabi.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import java.util.ArrayList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class LogicTxnTest {
  private LogicTxnTest() {}

  @Nested
  class TestTxn {
    @Test
    void testLogicTxnAndOk() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        runner
            .beginTxn()
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
            .endTxn();
      } catch (Err err) {
        fail(err);
      }

      assertThat(logger.stream().map(TestUtils::blackOutLineNo).toList())
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
    void testLogicRunButFailToRunButRunForceRuns() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        runner
            .beginTxn()
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
                  var conn = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("execute logic-2");
                })
            .runForce(
                (DataAcc data) -> {
                  var conn = data.getDataConn("foo", SyncDataConn.class);
                  logger.add("execute logic-3");
                })
            .endTxn();
      } catch (Err err) {
        switch (err.getReason()) {
          case DataHubError.FailToRunLogic r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index()).isEqualTo(0);
            assertThat(r.errors().get(0).name()).isEqualTo("LogicTxn#run(logic-0)");
            assertThat(r.errors().get(0).err().getReason()).isEqualTo("logic-0 failed");
          }
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
              "execute logic-0",
              "execute logic-3",
              "SyncDataConn#rollback 1",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = java.lang.String logic-0 failed, file = LogicTxnTest.java, line = *** }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testRunnerButFailedToRunOrBlockThenSkipEvenRunForce() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        runner
            .beginTxn()
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
                  var conn = data.getDataConn("foo", SyncDataConn.class);
                  logger.add("execute logic-2");
                })
            .runOrBlock(
                (DataAcc data) -> {
                  var conn = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("execute logic-3");
                })
            .endTxn();
      } catch (Err err) {
        switch (err.getReason()) {
          case DataHubError.FailToRunLogic r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index()).isEqualTo(0);
            assertThat(r.errors().get(0).name()).isEqualTo("LogicTxn#runOrBlock(logic-0)");
            assertThat(r.errors().get(0).err().getReason()).isEqualTo("logic-0 failed");
          }
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
              "execute logic-0",
              "SyncDataConn#rollback 1",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = java.lang.String logic-0 failed, file = LogicTxnTest.java, line = *** }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testRunnerAndFailedToRunForceThenSkipRunButRunForceRuns() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        runner
            .beginTxn()
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
                  var conn = data.getDataConn("foo", SyncDataConn.class);
                  logger.add("execute logic-2");
                })
            .runOrBlock(
                (DataAcc data) -> {
                  var conn = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("execute logic-3");
                })
            .endTxn();
      } catch (Err err) {
        switch (err.getReason()) {
          case DataHubError.FailToRunLogic r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index()).isEqualTo(0);
            assertThat(r.errors().get(0).name()).isEqualTo("LogicTxn#runForce(logic-0)");
            assertThat(r.errors().get(0).err().getReason()).isEqualTo("logic-0 failed");
          }
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
              "execute logic-0",
              "execute logic-2",
              "SyncDataConn#rollback 1",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runForce(logic-0), err=com.github.sttk.errs.Err { reason = java.lang.String logic-0 failed, file = LogicTxnTest.java, line = *** }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testRunnerAndFailedToCommit() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.Commit));
        runner.uses("baz", new SyncDataSrc(3, logger, Fail.None));

        runner
            .beginTxn()
            .runForce(
                (DataHub data) -> {
                  var conn = data.getDataConn("foo", SyncDataConn.class);
                  logger.add("execute logic-0");
                })
            .run(
                (DataHub data) -> {
                  var conn = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("execute logic-1");
                })
            .runForce(
                (DataHub data) -> {
                  var conn = data.getDataConn("baz", SyncDataConn.class);
                  logger.add("execute logic-2");
                })
            .runOrBlock(
                (DataHub data) -> {
                  var conn = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("execute logic-3");
                })
            .endTxn();
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
              "execute logic-0",
              "SyncDataSrc#createDataConn 2",
              "SyncDataConn#new 2",
              "execute logic-1",
              "SyncDataSrc#createDataConn 3",
              "SyncDataConn#new 3",
              "execute logic-2",
              "execute logic-3",
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
  }

  @Nested
  class TestNestedLogicRunningInDataAcc {
    interface HogeData {
      String getText() throws Err;

      void setText(String text) throws Err;
    }

    interface FugaData {
      void processText() throws Err;
    }

    static Logic<HogeData> hogeLogic =
        data -> {
          var text = data.getText();
          text = text.toUpperCase();
          data.setText(text);
        };

    static Logic<FugaData> fugaLogic =
        data -> {
          data.processText();
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

    interface GetSetDataAcc extends FugaData, DataAcc {
      @Override
      default void processText() throws Err {
        start().run(hogeLogic).end();
      }
    }

    class MyDataHub extends DataHub implements GettingDataAcc, SettingDataAcc, GetSetDataAcc {}

    @Test
    void textTxn() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new MyDataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        runner.beginTxn().run(fugaLogic).endTxn();
      } catch (Err err) {
        fail(err);
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
    void runsIfAbnormal() {
      try (var runner = new LogicRunner(new DataHub())) {
        runner.hub.run.localDataSrcManager.isAbnormal = true;
        runner.beginTxn().run(data -> {}).runForce(data -> {}).runOrBlock(data -> {}).endTxn();
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
            .beginTxn()
            .run(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but Error");
                  throw new Error("error");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("error");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
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
            .beginTxn()
            .run(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but RuntimeException");
                  throw new RuntimeException("error");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("error");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.RuntimeException: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.RuntimeException: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.RuntimeException: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.RuntimeException: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
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
            .beginTxn()
            .runForce(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but Error");
                  throw new Error("error");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("error");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
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
            .beginTxn()
            .runForce(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but RuntimeException");
                  throw new RuntimeException("error");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("error");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.RuntimeException: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.RuntimeException: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.RuntimeException: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.RuntimeException: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
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
            .beginTxn()
            .runOrBlock(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but Error");
                  throw new Error("error");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("error");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
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
            .beginTxn()
            .runOrBlock(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but RuntimeException");
                  throw new RuntimeException("error");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("error");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.RuntimeException: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.RuntimeException: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.RuntimeException: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#runOrBlock(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.RuntimeException: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]",
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

        runner
            .beginTxn()
            .run(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("preCommit Error");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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

        runner
            .beginTxn()
            .run(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("preCommit RuntimeException");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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

        runner
            .beginTxn()
            .run(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but Error");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("commit Error");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.CommitRuntimeException));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        runner
            .beginTxn()
            .run(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but Error");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("commit RuntimeException");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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
              "SyncDataConn#preCommit 1",
              "SyncDataConn#preCommit 2",
              "SyncDataConn#commit 1",
              "SyncDataConn#commit 2 RuntimeException",
              "SyncDataConn#rollback 2",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: commit RuntimeException }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: commit RuntimeException }} rollback:{State:NoneByRolledBack Err:null}}]",
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

        runner
            .beginTxn()
            .run(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but Error");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("postCommit Error");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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

        runner
            .beginTxn()
            .run(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but Error");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("postCommit RuntimeException");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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

        runner
            .beginTxn()
            .run(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but Error");
                  throw new Error("error");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("rollback Error");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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
              "SyncDataConn#rollback 1 Error",
              "SyncDataConn#rollback 2 Error",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.Error: rollback Error }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.Error: rollback Error }}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.Error: rollback Error }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.Error: rollback Error }}}]",
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

        runner
            .beginTxn()
            .run(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but Error");
                  throw new Error("error");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("rollback RuntimeException");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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
              "SyncDataConn#rollback 1 RuntimeException",
              "SyncDataConn#rollback 2 RuntimeException",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: rollback RuntimeException }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: rollback RuntimeException }}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: rollback RuntimeException }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$FailToRunLogic FailToRunLogic[errors=[ErrEntry[index=0, name=LogicTxn#run(logic-0), err=com.github.sttk.errs.Err { reason = com.github.sttk.sabi.LogicTxnError$ErrorOccurred ErrorOccurred[], file = LogicTxn.java, line = ***, cause = java.lang.Error: error }]]], file = LogicTxn.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = ***, cause = java.lang.RuntimeException: rollback RuntimeException }}}]",
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
            .beginTxn()
            .run(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("close Error");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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

        runner
            .beginTxn()
            .run(
                (DataAcc data) -> {
                  var conn1 = data.getDataConn("foo", SyncDataConn.class);
                  var conn2 = data.getDataConn("bar", SyncDataConn.class);
                  logger.add("Run the logic but Error");
                })
            .endTxn();
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("close RuntimeException");
      }

      assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
      assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
      assertThat(runner.hub.run.dataSrcMap).isEmpty();
      assertThat(runner.hub.run.dataConnManager.list).isEmpty();
      assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
      assertThat(runner.hub.run.fixed).isTrue();

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
