package com.github.sttk.sabi;

import static com.github.sttk.sabi.TestDoubles.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import java.util.ArrayList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DataAccTest {
  private DataAccTest() {}

  @Nested
  class TestGetDataConn {
    @Test
    void testGetDataConnByCreatingButFailedToCast() {
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

        try {
          runner.txn(
              (DataHub data) -> {
                var conn1 = data.getDataConn("foo", SyncDataConn.class);
                var conn2 = data.getDataConn("bar", AsyncDataConn.class);
                logger.add("Run the logic");
              });
        } catch (Err err) {
          switch (err.getReason()) {
            case DataSrcError.FailToCastCreatedDataConn r -> {
              assertThat(r.name()).isEqualTo("bar");
              assertThat(r.fromTypeName()).isEqualTo(SyncDataConn.class.getName());
              assertThat(r.toTypeName()).isEqualTo(AsyncDataConn.class.getName());
            }
            default -> fail(err);
          }
        }

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();
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
              "SyncDataConn#rollback 1",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataSrcError$FailToCastCreatedDataConn FailToCastCreatedDataConn[name=bar, fromTypeName=com.github.sttk.sabi.TestDoubles$SyncDataConn, toTypeName=com.github.sttk.sabi.TestDoubles$AsyncDataConn], file = DataSrcContainer.java, line = 36, cause = java.lang.ClassCastException: Cannot cast com.github.sttk.sabi.TestDoubles$SyncDataConn to com.github.sttk.sabi.TestDoubles$AsyncDataConn }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testGetDataConnByUsingCache() {
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

        runner.txn(
            (DataHub data) -> {
              var conn1 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);

              var conn11 = data.getDataConn("foo", SyncDataConn.class);
              var conn22 = data.getDataConn("bar", SyncDataConn.class);
              logger.add("Run the logic");
            });

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.list).hasSize(0);
        assertThat(runner.hub.run.dataConnManager.indexMap).hasSize(0);
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
    void testGetDataConnByUsingCacheButFailedToCast() {
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

        try {
          runner.txn(
              (DataHub data) -> {
                var conn1 = data.getDataConn("foo", SyncDataConn.class);
                var conn2 = data.getDataConn("bar", SyncDataConn.class);

                var conn22 = data.getDataConn("bar", AsyncDataConn.class);
                logger.add("Run the logic");
              });
        } catch (Err err) {
          switch (err.getReason()) {
            case DataConnError.FailToCastDataConn r -> {
              assertThat(r.name()).isEqualTo("bar");
              assertThat(r.fromTypeName()).isEqualTo(SyncDataConn.class.getName());
              assertThat(r.toTypeName()).isEqualTo(AsyncDataConn.class.getName());
            }
            default -> fail(err);
          }
        }

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.list).hasSize(0);
        assertThat(runner.hub.run.dataConnManager.indexMap).hasSize(0);
        assertThat(runner.hub.run.fixed).isFalse();
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
              "SyncDataConn#rollback 1",
              "SyncDataConn#rollback 2",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataConnError$FailToCastDataConn FailToCastDataConn[name=bar, fromTypeName=com.github.sttk.sabi.TestDoubles$SyncDataConn, toTypeName=com.github.sttk.sabi.TestDoubles$AsyncDataConn], file = DataConnManager.java, line = 68, cause = java.lang.ClassCastException: Cannot cast com.github.sttk.sabi.TestDoubles$SyncDataConn to com.github.sttk.sabi.TestDoubles$AsyncDataConn }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataConnError$FailToCastDataConn FailToCastDataConn[name=bar, fromTypeName=com.github.sttk.sabi.TestDoubles$SyncDataConn, toTypeName=com.github.sttk.sabi.TestDoubles$AsyncDataConn], file = DataConnManager.java, line = 68, cause = java.lang.ClassCastException: Cannot cast com.github.sttk.sabi.TestDoubles$SyncDataConn to com.github.sttk.sabi.TestDoubles$AsyncDataConn }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#onTxnFailure 2",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataConnError$FailToCastDataConn FailToCastDataConn[name=bar, fromTypeName=com.github.sttk.sabi.TestDoubles$SyncDataConn, toTypeName=com.github.sttk.sabi.TestDoubles$AsyncDataConn], file = DataConnManager.java, line = 68, cause = java.lang.ClassCastException: Cannot cast com.github.sttk.sabi.TestDoubles$SyncDataConn to com.github.sttk.sabi.TestDoubles$AsyncDataConn }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataConnError$FailToCastDataConn FailToCastDataConn[name=bar, fromTypeName=com.github.sttk.sabi.TestDoubles$SyncDataConn, toTypeName=com.github.sttk.sabi.TestDoubles$AsyncDataConn], file = DataConnManager.java, line = 68, cause = java.lang.ClassCastException: Cannot cast com.github.sttk.sabi.TestDoubles$SyncDataConn to com.github.sttk.sabi.TestDoubles$AsyncDataConn }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 2",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testGetDataConnButNoCorrespondingDataSrc() {
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

        try {
          runner.txn(
              (DataHub data) -> {
                var conn1 = data.getDataConn("foo", SyncDataConn.class);
                var conn2 = data.getDataConn("xxx", SyncDataConn.class);
                logger.add("Run the logic");
              });
        } catch (Err err) {
          switch (err.getReason()) {
            case DataHubError.NoDataSrcToCreateDataConn r -> {
              assertThat(r.name()).isEqualTo("xxx");
              assertThat(r.typeName()).isEqualTo(SyncDataConn.class.getName());
            }
            default -> fail(err);
          }
        }

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.list).hasSize(0);
        assertThat(runner.hub.run.dataConnManager.indexMap).hasSize(0);
        assertThat(runner.hub.run.fixed).isFalse();
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataConn#rollback 1",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataHubError$NoDataSrcToCreateDataConn NoDataSrcToCreateDataConn[name=xxx, typeName=com.github.sttk.sabi.TestDoubles$SyncDataConn], file = DataRun.java, line = 112 }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }

    @Test
    void testGetDataConnButFailToCreateDataConn() {
      var logger = new ArrayList<String>();

      var runner = new LogicRunner(new DataHub());
      try (runner) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.CreateDataConn));

        assertThat(runner.hub.run.localDataSrcManager.listUnready).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.listReady).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).isEmpty();
        assertThat(runner.hub.run.dataConnManager.list).isEmpty();
        assertThat(runner.hub.run.dataConnManager.indexMap).isEmpty();
        assertThat(runner.hub.run.fixed).isFalse();

        try {
          runner.txn(
              (DataHub data) -> {
                var conn1 = data.getDataConn("foo", SyncDataConn.class);
                var conn2 = data.getDataConn("bar", SyncDataConn.class);
                logger.add("Run the logic");
              });
        } catch (Err err) {
          switch (err.getReason()) {
            case DataSrcError.FailToCreateDataConn r -> {
              assertThat(r.name()).isEqualTo("bar");
              assertThat(r.typeName()).isEqualTo(SyncDataConn.class.getName());
            }
            default -> fail(err);
          }
        }

        assertThat(runner.hub.run.localDataSrcManager.listUnready).isEmpty();
        assertThat(runner.hub.run.localDataSrcManager.listReady).hasSize(2);
        assertThat(runner.hub.run.localDataSrcManager.local).isTrue();
        assertThat(runner.hub.run.dataSrcMap).hasSize(2);
        assertThat(runner.hub.run.dataConnManager.list).hasSize(0);
        assertThat(runner.hub.run.dataConnManager.indexMap).hasSize(0);
        assertThat(runner.hub.run.fixed).isFalse();
      }

      assertThat(logger)
          .containsExactly(
              "SyncDataSrc#new 1",
              "SyncDataSrc#new 2",
              "SyncDataSrc#setup 1",
              "SyncDataSrc#setup 2",
              "SyncDataSrc#createDataConn 1",
              "SyncDataConn#new 1",
              "SyncDataSrc#createDataConn 2 failed",
              "SyncDataConn#rollback 1",
              "SyncDataConn#onTxnFailure 1",
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataSrcError$FailToCreateDataConn FailToCreateDataConn[name=bar, typeName=com.github.sttk.sabi.TestDoubles$SyncDataConn], file = DataSrcContainer.java, line = 25, cause = com.github.sttk.errs.Err { reason = java.lang.String XXX, file = TestDoubles.java, line = 345 } }} rollback:{State:NoneByRolledBack Err:null}}]",
              "SyncDataConn#close 1",
              "SyncDataSrc#close 2",
              "SyncDataSrc#close 1");
    }
  }

  @Nested
  class TestAggregation {

    interface HogeData {
      String getText() throws Err;

      void setText(String text) throws Err;
    }

    interface GettingDataAcc extends DataAcc, HogeData {
      default String getText() throws Err {
        var conn = getDataConn("foo", SyncDataConn.class);
        return "hello";
      }
    }

    interface SettingDataAcc extends DataAcc, HogeData {
      default void setText(String text) throws Err {
        var conn = getDataConn("bar", SyncDataConn.class);
      }
    }

    class MyDataHub extends DataHub implements HogeData, GettingDataAcc, SettingDataAcc {}

    Logic<HogeData> hogeLogic =
        data -> {
          var text = data.getText();
          text = text.toLowerCase();
          data.setText(text);
        };

    @Test
    void testRun() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new MyDataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        runner.run(hogeLogic);
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
    void textTxn() {
      var logger = new ArrayList<String>();

      try (var runner = new LogicRunner(new MyDataHub())) {
        runner.uses("foo", new SyncDataSrc(1, logger, Fail.None));
        runner.uses("bar", new SyncDataSrc(2, logger, Fail.None));

        runner.txn(hogeLogic);
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
  }

  @Nested
  class TestNestedLogicRunning {}
}
