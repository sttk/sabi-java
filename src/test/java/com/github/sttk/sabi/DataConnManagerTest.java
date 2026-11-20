package com.github.sttk.sabi;

import static com.github.sttk.sabi.TestDoubles.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DataConnManagerTest {
  private DataConnManagerTest() {}

  @Nested
  class NewAndAdd {
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
  }

  @Nested
  class Utils {
    @Test
    void testFindByNameAndCastButNone() {
      var manager = new DataConnManager();

      try {
        assertThat(manager.findByNameAndCast("foo", SyncDataConn.class)).isNull();
        assertThat(manager.findByNameAndCast("bar", AsyncDataConn.class)).isNull();
      } catch (Exception e) {
        fail(e);
      }
    }

    @Test
    void testFindByNameAndCastAndFound() {
      var logger = new ArrayList<String>();

      var manager = new DataConnManager();

      var conn1 = new SyncDataConn(1, logger, Fail.None);
      var cont1 = new DataConnContainer("foo", conn1);
      manager.add(cont1);

      var conn2 = new AsyncDataConn(2, logger, Fail.None);
      var cont2 = new DataConnContainer("bar", conn2);
      manager.add(cont2);

      try {
        assertThat(manager.findByNameAndCast("foo", SyncDataConn.class)).isEqualTo(conn1);
        assertThat(manager.findByNameAndCast("bar", AsyncDataConn.class)).isEqualTo(conn2);
      } catch (Exception e) {
        fail(e);
      }
    }

    @Test
    void testFindByNameAndCastButFailToCast() {
      var logger = new ArrayList<String>();

      var manager = new DataConnManager();

      var conn1 = new SyncDataConn(1, logger, Fail.None);
      var cont1 = new DataConnContainer("foo", conn1);
      manager.add(cont1);

      var conn2 = new AsyncDataConn(2, logger, Fail.None);
      var cont2 = new DataConnContainer("bar", conn2);
      manager.add(cont2);

      try {
        manager.findByNameAndCast("foo", AsyncDataConn.class);
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case DataConnError.FailToCastDataConn r -> {
            assertThat(r.name()).isEqualTo("foo");
            assertThat(r.fromTypeName()).isEqualTo(SyncDataConn.class.getName());
            assertThat(r.toTypeName()).isEqualTo(AsyncDataConn.class.getName());
          }
          default -> fail(err);
        }
      }

      try {
        manager.findByNameAndCast("bar", SyncDataConn.class);
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case DataConnError.FailToCastDataConn r -> {
            assertThat(r.name()).isEqualTo("bar");
            assertThat(r.fromTypeName()).isEqualTo(AsyncDataConn.class.getName());
            assertThat(r.toTypeName()).isEqualTo(SyncDataConn.class.getName());
          }
          default -> fail(err);
        }
      }
    }

    @Test
    void testNewTxnFailureReportBuildersWithNoLogicErr() {
      var logger = new ArrayList<String>();

      var manager = new DataConnManager();

      var list = manager.newTxnFailureReportBuilders(null);
      assertThat(list).isEmpty();

      var reports = list.stream().map(b -> b.build()).collect(Collectors.toList());
      assertThat(reports).isEmpty();

      //

      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      list = manager.newTxnFailureReportBuilders(null);
      assertThat(list).hasSize(2);

      reports = list.stream().map(b -> b.build()).collect(Collectors.toList());
      assertThat(reports.get(0).dataConnName).isEqualTo("foo");
      assertThat(reports.get(0).dataConnType).isEqualTo(SyncDataConn.class.getName());
      assertThat(reports.get(0).cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
      assertThat(reports.get(0).cause.err).isNull();
      assertThat(reports.get(1).dataConnName).isEqualTo("bar");
      assertThat(reports.get(1).dataConnType).isEqualTo(SyncDataConn.class.getName());
      assertThat(reports.get(1).cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
      assertThat(reports.get(1).cause.err).isNull();
    }

    @Test
    void testNewTxnFailureReportBuildersWithLogicErr() {
      var logger = new ArrayList<String>();

      var err = new Err("logic error");

      var manager = new DataConnManager();

      var list = manager.newTxnFailureReportBuilders(err);
      assertThat(list).isEmpty();

      //

      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      var conn2 = new SyncDataConn(2, logger, Fail.None);
      manager.add(new DataConnContainer("bar", conn2));

      list = manager.newTxnFailureReportBuilders(err);
      assertThat(list).hasSize(2);

      var reports = list.stream().map(b -> b.build()).collect(Collectors.toList());
      assertThat(reports.get(0).dataConnName).isEqualTo("foo");
      assertThat(reports.get(0).dataConnType).isEqualTo(SyncDataConn.class.getName());
      assertThat(reports.get(0).cause.state).isEqualTo(TxnFailureCauseState.LogicFailure);
      assertThat(reports.get(0).cause.err).isEqualTo(err);
      assertThat(reports.get(1).dataConnName).isEqualTo("bar");
      assertThat(reports.get(1).dataConnType).isEqualTo(SyncDataConn.class.getName());
      assertThat(reports.get(1).cause.state).isEqualTo(TxnFailureCauseState.LogicFailure);
      assertThat(reports.get(1).cause.err).isEqualTo(err);
    }

    @Test
    void testSkipNoDataConn() {
      var logger = new ArrayList<String>();

      var manager = new DataConnManager();

      var conn1 = new SyncDataConn(1, logger, Fail.None);
      manager.add(new DataConnContainer("foo", conn1));

      manager.add(new DataConnContainer("bar", null));

      var conn3 = new SyncDataConn(3, logger, Fail.None);
      manager.add(new DataConnContainer("baz", conn3));

      var list = manager.newTxnFailureReportBuilders(null);
      assertThat(list).hasSize(2);

      var reports = list.stream().map(b -> b.build()).collect(Collectors.toList());
      assertThat(reports.get(0).dataConnName).isEqualTo("foo");
      assertThat(reports.get(0).dataConnType).isEqualTo(SyncDataConn.class.getName());
      assertThat(reports.get(0).cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
      assertThat(reports.get(0).cause.err).isNull();
      assertThat(reports.get(1).dataConnName).isEqualTo("baz");
      assertThat(reports.get(1).dataConnType).isEqualTo(SyncDataConn.class.getName());
      assertThat(reports.get(1).cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
      assertThat(reports.get(1).cause.err).isNull();
    }
  }

  @Nested
  class CommitAndRollback {
    @Test
    void testCommitAndRollbackOk() {
      var logger = new ArrayList<String>();

      var manager = new DataConnManager();
      try {
        var conn1 = new SyncDataConn(1, logger, Fail.None);
        manager.add(new DataConnContainer("foo", conn1));

        var conn2 = new AsyncDataConn(2, logger, Fail.None);
        manager.add(new DataConnContainer("bar", conn2));

        var builders = manager.newTxnFailureReportBuilders(null);

        manager.commit(builders);
        manager.rollback(builders);
      } catch (Err err) {
        fail(err);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(14);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#postCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        try {
          manager.commit(builders);
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case DataConnError.FailToPreCommitDataConn rsn -> {
              assertThat(rsn.errors()).hasSize(1);
              var ee = rsn.errors().get(0);
              assertThat(ee.index).isEqualTo(0);
              assertThat(ee.name).isEqualTo("foo");
              assertThat(ee.err.getReason()).isEqualTo("zzz");
            }
            default -> fail(err);
          }
          manager.rollback(builders);
        } catch (Exception e) {
          fail(e);
        }
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(11);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1 failed");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestDoubles.java, line = 74 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestDoubles.java, line = 74 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        try {
          manager.commit(builders);
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case DataConnError.FailToPreCommitDataConn rsn -> {
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
          manager.rollback(builders);
        } catch (Exception e) {
          fail(e);
        }
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(12);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2 failed");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 1 failed");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestDoubles.java, line = 190 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestDoubles.java, line = 74 }} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestDoubles.java, line = 190 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String zzz, file = TestDoubles.java, line = 74 }} rollback:{State:NoneByRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        try {
          manager.commit(builders);
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case DataConnError.FailToPreCommitDataConn rsn -> {
              assertThat(rsn.errors()).hasSize(1);
              var ee = rsn.errors().get(0);
              assertThat(ee.index).isEqualTo(1);
              assertThat(ee.name).isEqualTo("bar");
              assertThat(ee.err.getReason()).isEqualTo("yyy");
              assertThat(ee.err.getCause()).isNull();
            }
            default -> fail(err);
          }
          manager.rollback(builders);
        } catch (Exception e) {
          fail(e);
        }
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(12);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2 failed");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestDoubles.java, line = 190 }} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestDoubles.java, line = 190 }} rollback:{State:NoneByRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        try {
          manager.commit(builders);
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case DataConnError.FailToCommitDataConn rsn -> {
              assertThat(rsn.errors()).hasSize(1);
              var ee = rsn.errors().get(0);
              assertThat(ee.index).isEqualTo(0);
              assertThat(ee.name).isEqualTo("foo");
              assertThat(ee.err.getReason()).isEqualTo("ZZZ");
            }
            default -> fail(err);
          }
          manager.rollback(builders);
        } catch (Exception e) {
          fail(e);
        }
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(13);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1 failed");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestDoubles.java, line = 58 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestDoubles.java, line = 58 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        try {
          manager.commit(builders);
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case DataConnError.FailToCommitDataConn rsn -> {
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
          manager.rollback(builders);
        } catch (Exception e) {
          fail(e);
        }
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(14);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 2 failed");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 1 failed");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestDoubles.java, line = 173 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestDoubles.java, line = 58 }} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestDoubles.java, line = 173 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestDoubles.java, line = 58 }} rollback:{State:NoneByRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        try {
          manager.commit(builders);
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case DataConnError.FailToCommitDataConn rsn -> {
              assertThat(rsn.errors()).hasSize(1);
              var ee = rsn.errors().get(0);
              assertThat(ee.index).isEqualTo(1);
              assertThat(ee.name).isEqualTo("bar");
              assertThat(ee.err.getReason()).isEqualTo("YYY");
            }
            default -> fail(err);
          }
          manager.rollback(builders);
        } catch (Exception e) {
          fail(e);
        }
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(13);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2 failed");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestDoubles.java, line = 173 }} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestDoubles.java, line = 173 }} rollback:{State:NoneByRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        try {
          manager.commit(builders);
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case DataConnError.FailToPostCommitDataConn rsn -> {
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
          manager.rollback(builders);
        } catch (Exception e) {
          fail(e);
        }
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(14);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1 failed");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#postCommit 2 failed");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = 92 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = 209 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = 92 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = 209 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        try {
          manager.commit(builders);
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case DataConnError.FailToPostCommitDataConn rsn -> {
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
          manager.rollback(builders);
        } catch (Exception e) {
          fail(e);
        }
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(14);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1 failed");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#postCommit 2 failed");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = 209 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = 92 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = 209 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = 92 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        try {
          manager.commit(builders);
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case DataConnError.FailToPostCommitDataConn rsn -> {
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
        manager.rollback(builders);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(14);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#postCommit 2 failed");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = 209 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = 209 }} rollback:{State:NoneByNotRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        try {
          manager.commit(builders);
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case DataConnError.FailToPostCommitDataConn rsn -> {
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
        manager.rollback(builders);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(21);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("NoCommitDataConn#new 3");
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
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = 209 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$NoCommitDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("NoCommitDataConn#onTxnFailure 3");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = 209 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$NoCommitDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String !!!, file = TestDoubles.java, line = 209 }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$NoCommitDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        try {
          manager.commit(builders);
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case DataConnError.FailToCommitDataConn rsn -> {
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
        manager.rollback(builders);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(13);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1 failed");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2 failed");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestDoubles.java, line = 58 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestDoubles.java, line = 230 }}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestDoubles.java, line = 58 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String yyy, file = TestDoubles.java, line = 230 }}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        try {
          manager.commit(builders);
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case DataConnError.FailToCommitDataConn rsn -> {
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
        manager.rollback(builders);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(13);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2 failed");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestDoubles.java, line = 173 }} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestDoubles.java, line = 173 }} rollback:{State:NoneByRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        manager.commit(builders);
        manager.rollback(builders);
      } catch (Exception e) {
        fail(e);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(12);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#postCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        try {
          manager.commit(builders);
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case DataConnError.FailToCommitDataConn rsn -> {
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
        manager.rollback(builders);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(12);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#commit 2 failed");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestDoubles.java, line = 173 }} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String YYY, file = TestDoubles.java, line = 173 }} rollback:{State:NoneByRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        manager.commit(builders);
        manager.rollback(builders);
      } catch (Exception e) {
        fail(e);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(21);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 3");
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
              "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:qux dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 3");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:qux dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:qux dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 3");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
    }
  }

  @Nested
  class OnlyRollback {
    @Test
    void testOnlyRollbackAndFirstIsSync() {
      var logger = new ArrayList<String>();

      var manager = new DataConnManager();
      try {
        var conn1 = new SyncDataConn(1, logger, Fail.None);
        manager.add(new DataConnContainer("foo", conn1));

        var conn2 = new AsyncDataConn(2, logger, Fail.None);
        manager.add(new DataConnContainer("bar", conn2));

        var builders = manager.newTxnFailureReportBuilders(null);

        manager.rollback(builders);
      } catch (Exception e) {
        fail(e);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(10);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        manager.rollback(builders);
      } catch (Exception e) {
        fail(e);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(10);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        manager.rollback(builders);
      } catch (Exception e) {
        fail(e);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(10);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        manager.rollback(builders);
      } catch (Exception e) {
        fail(e);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(10);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1 failed");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestDoubles.java, line = 112 }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestDoubles.java, line = 112 }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$AsyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("AsyncDataConn#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
    }
  }

  @Nested
  class Panic {
    @Test
    void testCommitAndRollbackButFailPreCommit_Error() {
      var logger = new ArrayList<String>();

      List<TxnFailureReport.Builder> builders = null;
      var manager = new DataConnManager();
      try {
        var conn1 = new SyncDataConn(1, logger, Fail.PreCommitError);
        manager.add(new DataConnContainer("foo", conn1));

        var conn2 = new SyncDataConn(2, logger, Fail.None);
        manager.add(new DataConnContainer("bar", conn2));

        builders = manager.newTxnFailureReportBuilders(null);
        manager.commit(builders);
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("preCommit Error");
        manager.rollback(builders);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(11);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1 Error");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 105, cause = java.lang.Error: preCommit Error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 105, cause = java.lang.Error: preCommit Error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testCommitAndRollbackButFailPreCommit_RuntimeException() {
      var logger = new ArrayList<String>();

      List<TxnFailureReport.Builder> builders = null;

      var manager = new DataConnManager();
      try {
        var conn1 = new SyncDataConn(1, logger, Fail.None);
        manager.add(new DataConnContainer("foo", conn1));

        var conn2 = new SyncDataConn(2, logger, Fail.PreCommitRuntimeException);
        manager.add(new DataConnContainer("bar", conn2));

        builders = manager.newTxnFailureReportBuilders(null);
        manager.commit(builders);
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("preCommit RuntimeException");
        manager.rollback(builders);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(12);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2 RuntimeException");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 101, cause = java.lang.RuntimeException: preCommit RuntimeException }} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PreCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 101, cause = java.lang.RuntimeException: preCommit RuntimeException }} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testCommitAndRollbackButFailCommit_Error() {
      var logger = new ArrayList<String>();

      List<TxnFailureReport.Builder> builders = null;

      var manager = new DataConnManager();
      try {
        var conn1 = new SyncDataConn(1, logger, Fail.CommitError);
        manager.add(new DataConnContainer("foo", conn1));

        var conn2 = new SyncDataConn(2, logger, Fail.None);
        manager.add(new DataConnContainer("bar", conn2));

        builders = manager.newTxnFailureReportBuilders(null);

        manager.commit(builders);
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("commit Error");
        manager.rollback(builders);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(13);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1 Error");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 149, cause = java.lang.Error: commit Error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 149, cause = java.lang.Error: commit Error }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testCommitAndRollbackButFailCommit_RuntimeException() {
      var logger = new ArrayList<String>();

      List<TxnFailureReport.Builder> builders = null;

      var manager = new DataConnManager();
      try {
        var conn1 = new SyncDataConn(1, logger, Fail.None);
        manager.add(new DataConnContainer("foo", conn1));

        var conn2 = new SyncDataConn(2, logger, Fail.CommitRuntimeException);
        manager.add(new DataConnContainer("bar", conn2));

        builders = manager.newTxnFailureReportBuilders(null);
        manager.commit(builders);
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("commit RuntimeException");
        manager.rollback(builders);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(13);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 2 RuntimeException");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 145, cause = java.lang.RuntimeException: commit RuntimeException }} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 145, cause = java.lang.RuntimeException: commit RuntimeException }} rollback:{State:NoneByRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testCommitAndRollbackButFailPostCommit_Error() {
      var logger = new ArrayList<String>();

      List<TxnFailureReport.Builder> builders = null;

      var manager = new DataConnManager();
      try {
        var conn1 = new SyncDataConn(1, logger, Fail.PostCommitError);
        manager.add(new DataConnContainer("foo", conn1));

        var conn2 = new SyncDataConn(2, logger, Fail.PostCommitError);
        manager.add(new DataConnContainer("bar", conn2));

        builders = manager.newTxnFailureReportBuilders(null);
        manager.commit(builders);
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("postCommit Error");
        manager.rollback(builders);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(14);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1 Error");
      assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 2 Error");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 196, cause = java.lang.Error: postCommit Error }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 196, cause = java.lang.Error: postCommit Error }} rollback:{State:NoneByNotRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 196, cause = java.lang.Error: postCommit Error }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 196, cause = java.lang.Error: postCommit Error }} rollback:{State:NoneByNotRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testCommitAndRollbackButFailPostCommit_RuntimeException() {
      var logger = new ArrayList<String>();

      List<TxnFailureReport.Builder> builders = null;

      var manager = new DataConnManager();
      try {
        var conn1 = new SyncDataConn(1, logger, Fail.PostCommitRuntimeException);
        manager.add(new DataConnContainer("foo", conn1));

        var conn2 = new SyncDataConn(2, logger, Fail.PostCommitRuntimeException);
        manager.add(new DataConnContainer("bar", conn2));

        builders = manager.newTxnFailureReportBuilders(null);
        manager.commit(builders);
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("postCommit RuntimeException");
        manager.rollback(builders);
      } finally {
        manager.close();
      }

      assertThat(logger).hasSize(14);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#commit 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 1 RuntimeException");
      assertThat(iter.next()).isEqualTo("SyncDataConn#postCommit 2 RuntimeException");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 190, cause = java.lang.RuntimeException: postCommit RuntimeException }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 190, cause = java.lang.RuntimeException: postCommit RuntimeException }} rollback:{State:NoneByNotRolledBack Err:null}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 190, cause = java.lang.RuntimeException: postCommit RuntimeException }} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:PostCommitFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 190, cause = java.lang.RuntimeException: postCommit RuntimeException }} rollback:{State:NoneByNotRolledBack Err:null}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        manager.rollback(builders);
        fail();
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("rollback Error");
      } finally {
        manager.abend();
      }

      assertThat(logger).hasSize(10);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1 Error");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2 Error");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 257, cause = java.lang.Error: rollback Error }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 257, cause = java.lang.Error: rollback Error }}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 257, cause = java.lang.Error: rollback Error }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$ErrorOccurred ErrorOccurred[], file = DataConnManager.java, line = 257, cause = java.lang.Error: rollback Error }}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        manager.rollback(builders);
        fail();
      } catch (Exception e) {
        assertThat(e.getMessage()).isEqualTo("rollback RuntimeException");
      } finally {
        manager.abend();
      }

      assertThat(logger).hasSize(10);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1 RuntimeException");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2 RuntimeException");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 252, cause = java.lang.RuntimeException: rollback RuntimeException }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 252, cause = java.lang.RuntimeException: rollback RuntimeException }}}]");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 2");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 252, cause = java.lang.RuntimeException: rollback RuntimeException }}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.AsyncGroupError$RuntimeExceptionOccurred RuntimeExceptionOccurred[], file = DataConnManager.java, line = 252, cause = java.lang.RuntimeException: rollback RuntimeException }}}]");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        manager.rollback(builders);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("onTxnFailure Error");
      } finally {
        manager.abend();
      }

      assertThat(logger).hasSize(8);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
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

        var builders = manager.newTxnFailureReportBuilders(null);

        manager.rollback(builders);
        fail();
      } catch (Exception e) {
        assertThat(e.getMessage()).isEqualTo("onTxnFailure RuntimeException");
      } finally {
        manager.abend();
      }

      assertThat(logger).hasSize(9);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#onTxnFailure 1");
      assertThat(iter.next())
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
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

      assertThat(logger).hasSize(4);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
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

        var builders = manager.newTxnFailureReportBuilders(null);
        manager.close();
      } catch (Exception e) {
        assertThat(e.getMessage()).isEqualTo("close RuntimeException");
      } finally {
        manager.abend();
      }

      assertThat(logger).hasSize(4);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
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

      assertThat(logger).hasSize(4);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
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

        var builders = manager.newTxnFailureReportBuilders(null);
        manager.abend();
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(4);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#new 2");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 1");
      assertThat(iter.next()).isEqualTo("SyncDataConn#close 2 RuntimeException");
      assertThat(iter.hasNext()).isFalse();
    }
  }

  @Nested
  class Etc {
    class MinDataConn implements DataConn {
      @Override
      public void commit(AsyncGroup ag) throws Err {}

      @Override
      public boolean isCommitted() {
        return false;
      }

      @Override
      public void rollback(AsyncGroup ag) {}

      @Override
      public void close() {}
    }

    @Test
    void testDataConnMinimumImplementation() {
      var ag = new AsyncGroup();
      var reports = new ArrayList<TxnFailureReport>();

      try {
        var conn = new MinDataConn();
        conn.preCommit(ag);
        conn.commit(ag);
        conn.isCommitted();
        conn.postCommit(ag);
        conn.rollback(ag);
        conn.onTxnFailure(ag, reports);
        conn.close();
      } catch (Exception e) {
      }
    }
  }
}
