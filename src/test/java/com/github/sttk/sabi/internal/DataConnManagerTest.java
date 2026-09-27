package com.github.sttk.sabi.internal;

import static com.github.sttk.sabi.DataConn.FailToCommitDataConn;
import static com.github.sttk.sabi.DataConn.FailToPostCommitDataConn;
import static com.github.sttk.sabi.DataConn.FailToPreCommitDataConn;
import static com.github.sttk.sabi.internal.TestCommonsTest.AsyncDataConn;
import static com.github.sttk.sabi.internal.TestCommonsTest.Fail;
import static com.github.sttk.sabi.internal.TestCommonsTest.SyncDataConn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.DataConn;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class DataConnManagerTest {
  private DataConnManagerTest() {}

  @Test
  void testNew() {
    var manager = new DataConnManager();
    assertThat(manager.list).isEmpty();
    assertThat(manager.indexMap).isEmpty();
  }

  @Test
  void testNewWithCommitOrder() {
    var manager = new DataConnManager(List.of("bar", "baz", "foo"));
    assertThat(manager.list).hasSize(3);
    assertThat(manager.list.get(0).conn).isNull();
    assertThat(manager.list.get(1).conn).isNull();
    assertThat(manager.list.get(2).conn).isNull();
    assertThat(manager.indexMap).hasSize(3);
    assertThat(manager.indexMap.get("foo")).isEqualTo(2);
    assertThat(manager.indexMap.get("bar")).isEqualTo(0);
    assertThat(manager.indexMap.get("baz")).isEqualTo(1);
  }

  @Test
  void testNewAndAdd() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    assertThat(manager.list).isEmpty();
    assertThat(manager.indexMap).isEmpty();

    var conn1 = new SyncDataConn(1, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn1));
    assertThat(manager.list).hasSize(1);
    assertThat(manager.indexMap).hasSize(1);
    assertThat(manager.indexMap.get("foo")).isEqualTo(0);
    assertThat(manager.list.get(0).conn).isEqualTo(conn1);

    var conn2 = new AsyncDataConn(2, logger, Fail.None);
    manager.add(new DataConnContainer("bar", conn2));
    assertThat(manager.list).hasSize(2);
    assertThat(manager.indexMap).hasSize(2);
    assertThat(manager.indexMap.get("foo")).isEqualTo(0);
    assertThat(manager.indexMap.get("bar")).isEqualTo(1);
    assertThat(manager.list.get(0).conn).isEqualTo(conn1);
    assertThat(manager.list.get(1).conn).isEqualTo(conn2);
  }

  @Test
  void testAndAddWhenOverlappingName() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    assertThat(manager.list).isEmpty();
    assertThat(manager.indexMap).isEmpty();

    var conn1 = new SyncDataConn(1, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn1));
    assertThat(manager.list).hasSize(1);
    assertThat(manager.indexMap).hasSize(1);
    assertThat(manager.indexMap.get("foo")).isEqualTo(0);
    assertThat(manager.list.get(0).conn).isEqualTo(conn1);

    var conn2 = new AsyncDataConn(2, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn2));
    assertThat(manager.list).hasSize(1);
    assertThat(manager.indexMap).hasSize(1);
    assertThat(manager.indexMap.get("foo")).isEqualTo(0);
    assertThat(manager.list.get(0).conn).isEqualTo(conn1);
  }

  @Test
  void testNewWithCommitOrderAndAdd() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager(List.of("bar", "baz", "foo"));
    assertThat(manager.list).hasSize(3);
    assertThat(manager.list.get(0).conn).isNull();
    assertThat(manager.list.get(1).conn).isNull();
    assertThat(manager.list.get(2).conn).isNull();
    assertThat(manager.indexMap).hasSize(3);
    assertThat(manager.indexMap.get("foo")).isEqualTo(2);
    assertThat(manager.indexMap.get("bar")).isEqualTo(0);
    assertThat(manager.indexMap.get("baz")).isEqualTo(1);

    var conn1 = new SyncDataConn(1, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn1));
    assertThat(manager.indexMap).hasSize(3);
    assertThat(manager.list.get(0).conn).isNull();
    assertThat(manager.list.get(1).conn).isNull();
    assertThat(manager.list.get(2).conn).isEqualTo(conn1);

    var conn2 = new AsyncDataConn(2, logger, Fail.None);
    manager.add(new DataConnContainer("bar", conn2));
    assertThat(manager.indexMap).hasSize(3);
    assertThat(manager.list.get(0).conn).isEqualTo(conn2);
    assertThat(manager.list.get(1).conn).isNull();
    assertThat(manager.list.get(2).conn).isEqualTo(conn1);

    var conn3 = new SyncDataConn(3, logger, Fail.None);
    manager.add(new DataConnContainer("qux", conn3));
    assertThat(manager.indexMap).hasSize(4);
    assertThat(manager.list.get(0).conn).isEqualTo(conn2);
    assertThat(manager.list.get(1).conn).isNull();
    assertThat(manager.list.get(2).conn).isEqualTo(conn1);
    assertThat(manager.list.get(3).conn).isEqualTo(conn3);
  }

  @Test
  void testNewWithCommitOrderAndAddWhenOverlappingName() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager(List.of("bar", "baz", "foo"));
    assertThat(manager.list).hasSize(3);
    assertThat(manager.list.get(0).conn).isNull();
    assertThat(manager.list.get(1).conn).isNull();
    assertThat(manager.list.get(2).conn).isNull();
    assertThat(manager.indexMap).hasSize(3);
    assertThat(manager.indexMap.get("foo")).isEqualTo(2);
    assertThat(manager.indexMap.get("bar")).isEqualTo(0);
    assertThat(manager.indexMap.get("baz")).isEqualTo(1);

    var conn1 = new SyncDataConn(1, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn1));
    assertThat(manager.indexMap).hasSize(3);
    assertThat(manager.list.get(0).conn).isNull();
    assertThat(manager.list.get(1).conn).isNull();
    assertThat(manager.list.get(2).conn).isEqualTo(conn1);

    var conn2 = new AsyncDataConn(2, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn2));
    assertThat(manager.indexMap).hasSize(3);
    assertThat(manager.list.get(0).conn).isNull();
    assertThat(manager.list.get(1).conn).isNull();
    assertThat(manager.list.get(2).conn).isEqualTo(conn1);

    var conn3 = new SyncDataConn(3, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn3));
    assertThat(manager.indexMap).hasSize(3);
    assertThat(manager.list.get(0).conn).isNull();
    assertThat(manager.list.get(1).conn).isNull();
    assertThat(manager.list.get(2).conn).isEqualTo(conn1);
  }

  @Test
  void testNewFailureReports() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();

    var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
    manager.prepareTxnFailureReportBuilders(reportBuilders);
    assertThat(reportBuilders).isEmpty();

    var conn1 = new SyncDataConn(1, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn1));

    var conn2 = new AsyncDataConn(2, logger, Fail.None);
    manager.add(new DataConnContainer("bar", conn2));

    reportBuilders = new ArrayList<TxnFailureReportBuilder>();
    manager.prepareTxnFailureReportBuilders(reportBuilders);
    assertThat(reportBuilders).hasSize(2);

    var reports = reportBuilders.stream().map(b -> b.build()).collect(Collectors.toList());
    assertThat(reports.get(0).dataConnName).isEqualTo("foo");
    assertThat(reports.get(0).dataConnType).isEqualTo(SyncDataConn.class.getName());
    assertThat(reports.get(1).dataConnName).isEqualTo("bar");
    assertThat(reports.get(1).dataConnType).isEqualTo(AsyncDataConn.class.getName());
  }

  @Test
  void testCommitAndRollbackOk() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      manager.commit(reportBuilders);
      manager.rollback(reportBuilders);
    } catch (Err err) {
      fail(err);
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(12);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#postCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitWithOrderAndRollbackOk() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager(List.of("bar", "baz", "foo"));
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      var conn3 = new SyncDataConn(3, logger, Fail.None);
      manager.add(new DataConnContainer("qux", conn3));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      manager.commit(reportBuilders);
      manager.rollback(reportBuilders);
    } catch (Err err) {
      fail(err);
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(18);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 3");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 3");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 3");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:qux dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:qux dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 3");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:qux dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 3");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailFirstSyncPreCommit() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.PreCommit);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.PreCommit);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPreCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(0);
            assertThat(ee.name).isEqualTo("foo");
            assertThat(ee.err.getReason()).isEqualTo("zzz");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(9);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1 failed");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestCommonsTest.java, line = 60 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestCommonsTest.java, line = 60 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailFirstAsyncPreCommit() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new AsyncDataConn(1, logger, Fail.PreCommit);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.PreCommit);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPreCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(2);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(1);
            assertThat(ee.name).isEqualTo("bar");
            assertThat(ee.err.getReason()).isEqualTo("zzz");
            ee = rsn.errors().get(1);
            assertThat(ee.index).isEqualTo(0);
            assertThat(ee.name).isEqualTo("foo");
            assertThat(ee.err.getReason()).isEqualTo("yyy");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(10);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2 failed");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 1 failed");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestCommonsTest.java, line = 159 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestCommonsTest.java, line = 60 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestCommonsTest.java, line = 159 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestCommonsTest.java, line = 60 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFirstSyncPreCommitThrowsRuntimeException() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.PreCommitByRuntimeException);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPreCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(0);
            assertThat(ee.name).isEqualTo("foo");
            assertThat(ee.err.getReason()).isInstanceOf(AsyncGroup.RuntimeExceptionOccured.class);
            assertThat(ee.err.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(ee.err.getCause().getMessage()).isEqualTo("RR");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(9);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1 runtime error");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccured RuntimeExceptionOccured[], file = AsyncGroupImpl.java, line = 58, cause = java.lang.RuntimeException: RR }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccured RuntimeExceptionOccured[], file = AsyncGroupImpl.java, line = 58, cause = java.lang.RuntimeException: RR }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailSecondPreCommit() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.PreCommit);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPreCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(1);
            assertThat(ee.name).isEqualTo("bar");
            assertThat(ee.err.getReason()).isEqualTo("yyy");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(10);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2 failed");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestCommonsTest.java, line = 159 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestCommonsTest.java, line = 159 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailFirstSyncCommit() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.Commit);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.Commit);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(0);
            assertThat(ee.name).isEqualTo("foo");
            assertThat(ee.err.getReason()).isEqualTo("ZZZ");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(11);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1 failed");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestCommonsTest.java, line = 46 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestCommonsTest.java, line = 46 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailFirstAsyncCommit() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new AsyncDataConn(1, logger, Fail.Commit);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.Commit);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(2);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(1);
            assertThat(ee.name).isEqualTo("bar");
            assertThat(ee.err.getReason()).isEqualTo("ZZZ");
            ee = rsn.errors().get(1);
            assertThat(ee.index).isEqualTo(0);
            assertThat(ee.name).isEqualTo("foo");
            assertThat(ee.err.getReason()).isEqualTo("YYY");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(12);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 2 failed");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 1 failed");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 138 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestCommonsTest.java, line = 46 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 138 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestCommonsTest.java, line = 46 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailSecondCommit() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.Commit);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(1);
            assertThat(ee.name).isEqualTo("bar");
            assertThat(ee.err.getReason()).isEqualTo("YYY");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(11);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2 failed");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 138 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 138 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFirstCommitThrowsRuntimeException() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.CommitByRuntimeException);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(0);
            assertThat(ee.name).isEqualTo("foo");
            assertThat(ee.err.getReason()).isInstanceOf(AsyncGroup.RuntimeExceptionOccured.class);
            assertThat(ee.err.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(ee.err.getCause().getMessage()).isEqualTo("R");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(11);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1 runtime error");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccured RuntimeExceptionOccured[], file = AsyncGroupImpl.java, line = 58, cause = java.lang.RuntimeException: R }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccured RuntimeExceptionOccured[], file = AsyncGroupImpl.java, line = 58, cause = java.lang.RuntimeException: R }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailFirstSyncPostCommit() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.PostCommit);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.PostCommit);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPostCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(2);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(0);
            assertThat(ee.name).isEqualTo("foo");
            assertThat(ee.err.getReason()).isEqualTo("!!!");
            ee = rsn.errors().get(1);
            assertThat(ee.index).isEqualTo(1);
            assertThat(ee.name).isEqualTo("bar");
            assertThat(ee.err.getReason()).isEqualTo("!!!");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(12);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1 failed");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#postCommit 2 failed");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 76 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 182 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 76 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 182 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailFirstAsyncPostCommit() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new AsyncDataConn(2, logger, Fail.PostCommit);
      manager.add(new DataConnContainer("bar", conn1));

      var conn2 = new SyncDataConn(1, logger, Fail.PostCommit);
      manager.add(new DataConnContainer("foo", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPostCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(2);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(1);
            assertThat(ee.name).isEqualTo("foo");
            assertThat(ee.err.getReason()).isEqualTo("!!!");
            ee = rsn.errors().get(1);
            assertThat(ee.index).isEqualTo(0);
            assertThat(ee.name).isEqualTo("bar");
            assertThat(ee.err.getReason()).isEqualTo("!!!");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(12);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1 failed");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#postCommit 2 failed");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 182 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 76 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 182 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 76 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailSecondPostCommit() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.PostCommit);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPostCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(1);
            assertThat(ee.name).isEqualTo("bar");
            assertThat(ee.err.getReason()).isEqualTo("!!!");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(12);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#postCommit 2 failed");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 182 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 182 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButSecondPostCommitThrowsRuntimException() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.PostCommitByRuntimeException);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPostCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(0);
            assertThat(ee.name).isEqualTo("foo");
            assertThat(ee.err.getReason()).isInstanceOf(AsyncGroup.RuntimeExceptionOccured.class);
            assertThat(ee.err.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(ee.err.getCause().getMessage()).isEqualTo("RRR");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(12);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1 runtime error");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#postCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccured RuntimeExceptionOccured[], file = AsyncGroupImpl.java, line = 58, cause = java.lang.RuntimeException: RRR }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccured RuntimeExceptionOccured[], file = AsyncGroupImpl.java, line = 58, cause = java.lang.RuntimeException: RRR }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testOnlyRollbackAndFirstIsSync() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      manager.rollback(reportBuilders);
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(8);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testOnlyRollbackAndFirstIsAsync() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new AsyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      manager.rollback(reportBuilders);
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(8);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testOnlyRollbackAndSecondRollbackFailed() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new AsyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.Rollback);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      manager.rollback(reportBuilders);
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(8);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2 failed");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestCommonsTest.java, line = 94 }}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestCommonsTest.java, line = 94 }}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testOnlyRollbackAndFirstRollbackFailed() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.Rollback);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      manager.rollback(reportBuilders);
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(8);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1 failed");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestCommonsTest.java, line = 94 }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestCommonsTest.java, line = 94 }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testOnlyRollbackAndFirstRollbackThrowsRuntimeException() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.RollbackByRuntimeException);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      manager.rollback(reportBuilders);
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(8);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1 runtime error");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccured RuntimeExceptionOccured[], file = AsyncGroupImpl.java, line = 58, cause = java.lang.RuntimeException: RRRR }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccured RuntimeExceptionOccured[], file = AsyncGroupImpl.java, line = 58, cause = java.lang.RuntimeException: RRRR }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackAndFirstRollbackFailedThenSecondRollbackFailed() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.Commit);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.Rollback);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(0);
            assertThat(ee.name).isEqualTo("foo");
            assertThat(ee.err.getReason()).isEqualTo("ZZZ");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(11);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1 failed");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2 failed");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestCommonsTest.java, line = 46 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestCommonsTest.java, line = 207 }}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestCommonsTest.java, line = 46 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestCommonsTest.java, line = 207 }}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackAndSecondCommitFailedThenFirstRollbackFailed() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.Rollback);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.Commit);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(1);
            assertThat(ee.name).isEqualTo("bar");
            assertThat(ee.err.getReason()).isEqualTo("YYY");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(11);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2 failed");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 138 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 138 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackAndPreCommitBecomeCommittedAndOk() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.PreCommitBecomeCommitted);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.PreCommitBecomeCommitted);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(10);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#postCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackAndPreCommitBecomeCommittedButFailed() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.PreCommitBecomeCommitted);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.Commit);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.prepareTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(1);
            assertThat(ee.name).isEqualTo("bar");
            assertThat(ee.err.getReason()).isEqualTo("YYY");
          }
          default -> fail(err);
        }
        manager.rollback(reportBuilders);
      } catch (Exception e) {
        fail(e);
      }
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(10);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2 failed");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 138 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 138 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testDefaultMethodOfDataConn() throws Err {
    var conn =
        new DataConn() {
          @Override
          public void commit(AsyncGroup ag) throws Err {}

          @Override
          public void rollback(AsyncGroup ag) throws Err {}

          @Override
          public void close() {}

          @Override
          public boolean isCommitted() {
            return true;
          }
        };

    conn.preCommit(null);
    conn.commit(null);
    conn.postCommit(null);
    conn.rollback(null);
    conn.onTxnFailure(null, null);
    conn.close();
  }
}
