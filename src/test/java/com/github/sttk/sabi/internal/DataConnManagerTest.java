package com.github.sttk.sabi.internal;

import static com.github.sttk.sabi.AsyncGroup.*;
import static com.github.sttk.sabi.DataConn.*;
import static com.github.sttk.sabi.internal.TestCommonsTest.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
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
    assertThat(manager.orderedNames).isEmpty();
    assertThat(manager.committed).isFalse();
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
    assertThat(manager.orderedNames).containsExactly("bar", "baz", "foo");
    assertThat(manager.committed).isFalse();
  }

  @Test
  void testWithEmptyCommitOrder() {
    var manager = new DataConnManager(new ArrayList<>());

    assertThat(manager.list).hasSize(0);
    assertThat(manager.indexMap).isEmpty();
    assertThat(manager.orderedNames).isEmpty();
    assertThat(manager.committed).isFalse();
  }

  @Test
  void testNewAndAdd() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();

    assertThat(manager.list).isEmpty();
    assertThat(manager.indexMap).isEmpty();
    assertThat(manager.orderedNames).isEmpty();
    assertThat(manager.committed).isFalse();

    var conn1 = new SyncDataConn(1, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn1));

    assertThat(manager.list).hasSize(1);
    assertThat(manager.list.get(0).conn).isEqualTo(conn1);
    assertThat(manager.indexMap).hasSize(1);
    assertThat(manager.indexMap.get("foo")).isEqualTo(0);

    var conn2 = new AsyncDataConn(2, logger, Fail.None);
    manager.add(new DataConnContainer("bar", conn2));

    assertThat(manager.list).hasSize(2);
    assertThat(manager.list.get(0).conn).isEqualTo(conn1);
    assertThat(manager.list.get(1).conn).isEqualTo(conn2);
    assertThat(manager.indexMap).hasSize(2);
    assertThat(manager.indexMap.get("foo")).isEqualTo(0);
    assertThat(manager.indexMap.get("bar")).isEqualTo(1);
  }

  @Test
  void testNewAndAddWhenOverlappingName() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    assertThat(manager.list).isEmpty();
    assertThat(manager.indexMap).isEmpty();
    assertThat(manager.orderedNames).isEmpty();
    assertThat(manager.committed).isFalse();

    var conn1 = new SyncDataConn(1, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn1));

    assertThat(manager.list).hasSize(1);
    assertThat(manager.list.get(0).conn).isEqualTo(conn1);
    assertThat(manager.indexMap).hasSize(1);
    assertThat(manager.indexMap.get("foo")).isEqualTo(0);
    assertThat(manager.orderedNames).isEmpty();
    assertThat(manager.committed).isFalse();

    var conn2 = new AsyncDataConn(2, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn2));

    assertThat(manager.list).hasSize(1);
    assertThat(manager.list.get(0).conn).isEqualTo(conn1);
    assertThat(manager.indexMap).hasSize(1);
    assertThat(manager.indexMap.get("foo")).isEqualTo(0);
    assertThat(manager.orderedNames).isEmpty();
    assertThat(manager.committed).isFalse();
  }

  @Test
  void testWithCommitOrderAndAdd() {
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
    assertThat(manager.orderedNames).containsExactly("bar", "baz", "foo");
    assertThat(manager.committed).isFalse();

    var conn1 = new SyncDataConn(1, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn1));

    assertThat(manager.list).hasSize(3);
    assertThat(manager.list.get(0).conn).isNull();
    assertThat(manager.list.get(1).conn).isNull();
    assertThat(manager.list.get(2).conn).isEqualTo(conn1);
    assertThat(manager.indexMap).hasSize(3);
    assertThat(manager.indexMap.get("foo")).isEqualTo(2);
    assertThat(manager.indexMap.get("bar")).isEqualTo(0);
    assertThat(manager.indexMap.get("baz")).isEqualTo(1);
    assertThat(manager.orderedNames).containsExactly("bar", "baz", "foo");
    assertThat(manager.committed).isFalse();

    var conn2 = new AsyncDataConn(2, logger, Fail.None);
    manager.add(new DataConnContainer("bar", conn2));

    assertThat(manager.list).hasSize(3);
    assertThat(manager.list.get(0).conn).isEqualTo(conn2);
    assertThat(manager.list.get(1).conn).isNull();
    assertThat(manager.list.get(2).conn).isEqualTo(conn1);
    assertThat(manager.indexMap).hasSize(3);
    assertThat(manager.indexMap.get("foo")).isEqualTo(2);
    assertThat(manager.indexMap.get("bar")).isEqualTo(0);
    assertThat(manager.indexMap.get("baz")).isEqualTo(1);
    assertThat(manager.orderedNames).containsExactly("bar", "baz", "foo");
    assertThat(manager.committed).isFalse();

    var conn3 = new AsyncDataConn(3, logger, Fail.None);
    manager.add(new DataConnContainer("qux", conn3));

    assertThat(manager.list).hasSize(4);
    assertThat(manager.list.get(0).conn).isEqualTo(conn2);
    assertThat(manager.list.get(1).conn).isNull();
    assertThat(manager.list.get(2).conn).isEqualTo(conn1);
    assertThat(manager.list.get(3).conn).isEqualTo(conn3);
    assertThat(manager.indexMap).hasSize(4);
    assertThat(manager.indexMap.get("foo")).isEqualTo(2);
    assertThat(manager.indexMap.get("bar")).isEqualTo(0);
    assertThat(manager.indexMap.get("baz")).isEqualTo(1);
    assertThat(manager.indexMap.get("qux")).isEqualTo(3);
    assertThat(manager.orderedNames).containsExactly("bar", "baz", "foo");
    assertThat(manager.committed).isFalse();
  }

  @Test
  void testWithCommitOrderAndAddWhenOverlappingName() {
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
    assertThat(manager.orderedNames).containsExactly("bar", "baz", "foo");
    assertThat(manager.committed).isFalse();

    var conn1 = new SyncDataConn(1, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn1));

    assertThat(manager.list).hasSize(3);
    assertThat(manager.list.get(0).conn).isNull();
    assertThat(manager.list.get(1).conn).isNull();
    assertThat(manager.list.get(2).conn).isEqualTo(conn1);
    assertThat(manager.indexMap).hasSize(3);
    assertThat(manager.indexMap.get("foo")).isEqualTo(2);
    assertThat(manager.indexMap.get("bar")).isEqualTo(0);
    assertThat(manager.indexMap.get("baz")).isEqualTo(1);
    assertThat(manager.orderedNames).containsExactly("bar", "baz", "foo");
    assertThat(manager.committed).isFalse();

    var conn2 = new SyncDataConn(2, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn2));

    assertThat(manager.list).hasSize(3);
    assertThat(manager.list.get(0).conn).isNull();
    assertThat(manager.list.get(1).conn).isNull();
    assertThat(manager.list.get(2).conn).isEqualTo(conn1);
    assertThat(manager.indexMap).hasSize(3);
    assertThat(manager.indexMap.get("foo")).isEqualTo(2);
    assertThat(manager.indexMap.get("bar")).isEqualTo(0);
    assertThat(manager.indexMap.get("baz")).isEqualTo(1);
    assertThat(manager.orderedNames).containsExactly("bar", "baz", "foo");
    assertThat(manager.committed).isFalse();
  }

  @Test
  void testInitTxnFailureReportBuilders() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();

    var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
    manager.initTxnFailureReportBuilders(reportBuilders);
    assertThat(reportBuilders).isEmpty();

    var conn1 = new SyncDataConn(1, logger, Fail.None);
    manager.add(new DataConnContainer("foo", conn1));

    var conn2 = new SyncDataConn(2, logger, Fail.None);
    manager.add(new DataConnContainer("bar", conn2));

    reportBuilders = new ArrayList<TxnFailureReportBuilder>();
    manager.initTxnFailureReportBuilders(reportBuilders);
    assertThat(reportBuilders).hasSize(2);

    var reports = reportBuilders.stream().map(b -> b.build()).collect(Collectors.toList());
    assertThat(reports.get(0).dataConnName).isEqualTo("foo");
    assertThat(reports.get(0).dataConnType).isEqualTo(SyncDataConn.class.getName());
    assertThat(reports.get(1).dataConnName).isEqualTo("bar");
    assertThat(reports.get(1).dataConnType).isEqualTo(SyncDataConn.class.getName());
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
      manager.initTxnFailureReportBuilders(reportBuilders);

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
  void testCommitAndRollbackButFailFirstSyncPreCommit() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.PreCommit);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.PreCommit);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
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
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestCommonsTest.java, line = 74 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestCommonsTest.java, line = 74 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
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
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
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
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestCommonsTest.java, line = 189 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestCommonsTest.java, line = 74 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestCommonsTest.java, line = 189 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestCommonsTest.java, line = 74 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 1");
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
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPreCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(1);
            assertThat(ee.name).isEqualTo("bar");
            assertThat(ee.err.getReason()).isEqualTo("yyy");
            assertThat(ee.err.getCause()).isNull();
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
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestCommonsTest.java, line = 189 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestCommonsTest.java, line = 189 }} rollback:{State:NoneByRolledBack Err:null}}]");
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
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
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
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestCommonsTest.java, line = 58 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestCommonsTest.java, line = 58 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
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
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
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
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 172 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestCommonsTest.java, line = 58 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 172 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestCommonsTest.java, line = 58 }} rollback:{State:NoneByRolledBack Err:null}}]");
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
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
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
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 172 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 172 }} rollback:{State:NoneByRolledBack Err:null}}]");
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
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
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
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 92 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 208 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 92 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 208 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
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
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
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
            "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 208 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 92 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 208 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 92 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
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
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
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
      } catch (Exception e) {
        fail(e);
      }
      manager.rollback(reportBuilders);
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
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 208 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 208 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailSecondPostCommitAndContainsNoCommit() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.PostCommit);
      manager.add(new DataConnContainer("bar", conn2));

      var conn3 = new NoCommitDataConn(3, logger);
      manager.add(new DataConnContainer("baz", conn3));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
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
      } catch (Exception e) {
        fail(e);
      }
      manager.rollback(reportBuilders);
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(18);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("NoCommitDataConn#preCommit 3");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
    assertThat(iter.next()).isEqualTo("NoCommitDataConn#commit 3");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1");
    assertThat(iter.next()).isEqualTo("NoCommitDataConn#postCommit 3");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#postCommit 2 failed");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 208 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$NoCommitDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("NoCommitDataConn#onTxnFailure 3");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 208 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$NoCommitDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestCommonsTest.java, line = 208 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$NoCommitDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("NoCommitDataConn#close 3");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackAndFirstCommitFailedAndSecondRollbackFailed() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.Commit);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.Rollback);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
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
      } catch (Exception e) {
        fail(e);
      }
      manager.rollback(reportBuilders);
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
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestCommonsTest.java, line = 58 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestCommonsTest.java, line = 229 }}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestCommonsTest.java, line = 58 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestCommonsTest.java, line = 229 }}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackAndSecondCommitFailedAndFirstRollbackFailed() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.Rollback);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.Commit);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
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
      } catch (Exception e) {
        fail(e);
      }
      manager.rollback(reportBuilders);
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
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 172 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 172 }} rollback:{State:NoneByRolledBack Err:null}}]");
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
      manager.initTxnFailureReportBuilders(reportBuilders);

      manager.commit(reportBuilders);
      manager.rollback(reportBuilders);
    } catch (Exception e) {
      fail(e);
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
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
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
      } catch (Exception e) {
        fail(e);
      }
      manager.rollback(reportBuilders);
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
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 172 }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestCommonsTest.java, line = 172 }} rollback:{State:NoneByRolledBack Err:null}}]");
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

      var conn2 = new AsyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      var conn3 = new SyncDataConn(3, logger, Fail.None);
      manager.add(new DataConnContainer("qux", conn3));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      manager.commit(reportBuilders);
      manager.rollback(reportBuilders);
    } catch (Exception e) {
      fail(e);
    } finally {
      manager.close();
    }

    assertThat(logger).hasSize(18);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 3");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 3");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 3");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#postCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:qux dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 3");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:qux dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:qux dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 3");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
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
      manager.initTxnFailureReportBuilders(reportBuilders);

      manager.rollback(reportBuilders);
    } catch (Exception e) {
      fail(e);
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
      manager.initTxnFailureReportBuilders(reportBuilders);

      manager.rollback(reportBuilders);
    } catch (Exception e) {
      fail(e);
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
  }

  @Test
  void testOnlyRollbackAndSecondRollbackFailed() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new AsyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      manager.rollback(reportBuilders);
    } catch (Exception e) {
      fail(e);
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
      manager.initTxnFailureReportBuilders(reportBuilders);

      manager.rollback(reportBuilders);
    } catch (Exception e) {
      fail(e);
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
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestCommonsTest.java, line = 112 }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestCommonsTest.java, line = 112 }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
  }

  @Test
  void testCommitAndRollbackButFailPreCommit_Error() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.PreCommitError);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPreCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(0);
            assertThat(ee.name).isEqualTo("foo");
            assertThat(ee.err.getReason()).isInstanceOf(ErrorOccurred.class);
            assertThat(ee.err.getCause()).isInstanceOf(Error.class);
            assertThat(ee.err.getCause().getMessage()).isEqualTo("preCommit Error");
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
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1 Error");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 89, cause = java.lang.Error: preCommit Error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 89, cause = java.lang.Error: preCommit Error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailPreCommit_RuntimeException() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.PreCommitRuntimeException);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPreCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(1);
            assertThat(ee.name).isEqualTo("bar");
            assertThat(ee.err.getReason()).isInstanceOf(RuntimeExceptionOccurred.class);
            assertThat(ee.err.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(ee.err.getCause().getMessage()).isEqualTo("preCommit RuntimeException");
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
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2 RuntimeException");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 86, cause = java.lang.RuntimeException: preCommit RuntimeException }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 86, cause = java.lang.RuntimeException: preCommit RuntimeException }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailCommit_Error() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.CommitError);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(0);
            assertThat(ee.name).isEqualTo("foo");
            assertThat(ee.err.getReason()).isInstanceOf(ErrorOccurred.class);
            assertThat(ee.err.getCause()).isInstanceOf(Error.class);
            assertThat(ee.err.getCause().getMessage()).isEqualTo("commit Error");
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
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1 Error");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 123, cause = java.lang.Error: commit Error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 123, cause = java.lang.Error: commit Error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailCommit_RuntimeException() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.CommitRuntimeException);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(1);
            assertThat(ee.name).isEqualTo("bar");
            assertThat(ee.err.getReason()).isInstanceOf(RuntimeExceptionOccurred.class);
            assertThat(ee.err.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(ee.err.getCause().getMessage()).isEqualTo("commit RuntimeException");
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
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 2 RuntimeException");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 120, cause = java.lang.RuntimeException: commit RuntimeException }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 120, cause = java.lang.RuntimeException: commit RuntimeException }} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailPostCommit_Error() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.PostCommitError);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPostCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(0);
            assertThat(ee.name).isEqualTo("foo");
            assertThat(ee.err.getReason()).isInstanceOf(ErrorOccurred.class);
            assertThat(ee.err.getCause()).isInstanceOf(Error.class);
            assertThat(ee.err.getCause().getMessage()).isEqualTo("postCommit Error");
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
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1 Error");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 158, cause = java.lang.Error: postCommit Error }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 158, cause = java.lang.Error: postCommit Error }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailPostCommit_RuntimeException() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.PostCommitRuntimeException);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      try {
        manager.commit(reportBuilders);
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPostCommitDataConn rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(1);
            assertThat(ee.name).isEqualTo("bar");
            assertThat(ee.err.getReason()).isInstanceOf(RuntimeExceptionOccurred.class);
            assertThat(ee.err.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(ee.err.getCause().getMessage()).isEqualTo("postCommit RuntimeException");
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
    assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#commit 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 2 RuntimeException");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 155, cause = java.lang.RuntimeException: postCommit RuntimeException }} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 155, cause = java.lang.RuntimeException: postCommit RuntimeException }} rollback:{State:NoneByNotRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailRollback_Error() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.RollbackError);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.RollbackError);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      manager.rollback(reportBuilders);
      fail();
    } catch (Error e) {
      assertThat(e.getMessage()).isEqualTo("rollback Error");
    } finally {
      manager.abend();
    }

    assertThat(logger).hasSize(8);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1 Error");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2 Error");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 208, cause = java.lang.Error: rollback Error }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 208, cause = java.lang.Error: rollback Error }}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 208, cause = java.lang.Error: rollback Error }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 208, cause = java.lang.Error: rollback Error }}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailRollback_RuntimeException() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.RollbackRuntimeException);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.RollbackRuntimeException);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      manager.rollback(reportBuilders);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("rollback RuntimeException");
    } finally {
      manager.abend();
    }

    assertThat(logger).hasSize(8);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1 RuntimeException");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2 RuntimeException");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 203, cause = java.lang.RuntimeException: rollback RuntimeException }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 203, cause = java.lang.RuntimeException: rollback RuntimeException }}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 203, cause = java.lang.RuntimeException: rollback RuntimeException }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroup$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 203, cause = java.lang.RuntimeException: rollback RuntimeException }}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailOnTxnFailure_Error() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.OnTxnFailureError);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.OnTxnFailureError);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      manager.rollback(reportBuilders);
    } catch (Error e) {
      assertThat(e.getMessage()).isEqualTo("onTxnFailure Error");
    } finally {
      manager.abend();
    }

    assertThat(logger).hasSize(6);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1 Error");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2 Error");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailOnTxnFailure_RuntimeException() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.OnTxnFailureRuntimeException);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);

      manager.rollback(reportBuilders);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("onTxnFailure RuntimeException");
    } finally {
      manager.abend();
    }

    assertThat(logger).hasSize(7);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
    assertThat(iter.next())
        .isEqualTo(
            "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.internal.TestCommonsTest$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2 RuntimeException");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailClose_Error() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.CloseError);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.CloseError);
      manager.add(new DataConnContainer("bar", conn2));
      manager.close();
    } catch (Error e) {
      assertThat(e.getMessage()).isEqualTo("close Error");
    } finally {
      manager.abend();
    }

    assertThat(logger).hasSize(2);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2 Error");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1 Error");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailClose_RuntimeException() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.CloseRuntimeException);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);
      manager.close();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("close RuntimeException");
    } finally {
      manager.abend();
    }

    assertThat(logger).hasSize(2);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2 RuntimeException");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailCloseInAbend_Error() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager();
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.CloseError);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.CloseError);
      manager.add(new DataConnContainer("bar", conn2));
      manager.abend();
    } catch (Exception e) {
      fail(e);
    }

    assertThat(logger).hasSize(2);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2 Error");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1 Error");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testCommitAndRollbackButFailCloseInAbend_RuntimeException() {
    var logger = new ArrayList<String>();

    var manager = new DataConnManager(List.of("bar", "baz", "foo"));
    try {
      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.CloseRuntimeException);
      manager.add(new DataConnContainer("bar", conn2));

      var reportBuilders = new ArrayList<TxnFailureReportBuilder>();
      manager.initTxnFailureReportBuilders(reportBuilders);
      manager.abend();
    } catch (Exception e) {
      fail(e);
    }

    assertThat(logger).hasSize(2);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    assertThat(iter.next()).isEqualTo("SyncDataConn#close 2 RuntimeException");
    assertThat(iter.hasNext()).isFalse();
  }
}
