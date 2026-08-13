package com.github.sttk.sabi.internal;

import static com.github.sttk.sabi.DataConn.FailToCommitDataConn;
import static com.github.sttk.sabi.DataConn.FailToPostCommitDataConn;
import static com.github.sttk.sabi.DataConn.FailToPreCommitDataConn;
import static com.github.sttk.sabi.DataHub.CreatedDataConnIsNull;
import static com.github.sttk.sabi.DataHub.FailToCastDataConn;
import static com.github.sttk.sabi.DataHub.FailToCastDataHub;
import static com.github.sttk.sabi.DataHub.FailToCreateDataConn;
import static com.github.sttk.sabi.DataHub.FailToSetupGlobalDataSrcs;
import static com.github.sttk.sabi.DataHub.FailToSetupLocalDataSrcs;
import static com.github.sttk.sabi.DataHub.NoDataSrcToCreateDataConn;
import static com.github.sttk.sabi.Sabi.setup;
import static com.github.sttk.sabi.Sabi.uses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.DataHub;
import com.github.sttk.sabi.DataSrc;
import com.github.sttk.sabi.TxnFailureReport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DataHubInnerTest {
  private DataHubInnerTest() {}

  enum Failure {
    None,
    PreCommit,
    Commit,
    PostCommit,
    Rollback,
    Setup,
    CreateDataConn,
    CreatedDataConnIsNull,
  }

  static class MyDataConn implements DataConn {
    int id;
    Failure failure;
    boolean committed;
    List<String> logger;

    MyDataConn(int id, Failure failure, List<String> logger) {
      this.id = id;
      this.failure = failure;
      this.logger = logger;
      this.committed = false;
    }

    @Override
    public boolean isCommitted() {
      return this.committed;
    }

    @Override
    public void preCommit(AsyncGroup ag) throws Err {
      if (this.failure == Failure.PreCommit) {
        this.logger.add(String.format("MyDataConn#preCommit %d failed", this.id));
        throw new Err("pre commit error");
      }
      this.logger.add(String.format("MyDataConn#preCommit %d", this.id));
    }

    @Override
    public void commit(AsyncGroup ag) throws Err {
      if (this.failure == Failure.Commit) {
        this.logger.add(String.format("MyDataConn#commit %d failed", this.id));
        throw new Err("commit error");
      }
      this.logger.add(String.format("MyDataConn#commit %d", this.id));
    }

    @Override
    public void postCommit(AsyncGroup ag) throws Err {
      if (this.failure == Failure.PostCommit) {
        this.logger.add(String.format("MyDataConn#postCommit %d failed", this.id));
        throw new Err("post commit error");
      }
      this.logger.add(String.format("MyDataConn#postCommit %d", this.id));
    }

    @Override
    public void rollback(AsyncGroup ag) throws Err {
      if (this.failure == Failure.Rollback) {
        this.logger.add(String.format("MyDataConn#rollback %d failed", this.id));
        throw new Err("rollback error");
      }
      this.logger.add(String.format("MyDataConn#rollback %d", this.id));
    }

    @Override
    public void onTxnFailure(AsyncGroup ag, List<TxnFailureReport> repots) {
      this.logger.add(String.format("MyDataConn#onTxnFailure %d", this.id));
    }

    @Override
    public void close() {
      this.logger.add(String.format("MyDataConn#close %d", this.id));
    }
  }

  static class MyDataSrc implements DataSrc {
    int id;
    Failure failure;
    List<String> logger;

    MyDataSrc(int id, Failure failure, List<String> logger) {
      this.id = id;
      this.failure = failure;
      this.logger = logger;
    }

    @Override
    public void setup(AsyncGroup ag) throws Err {
      if (this.failure == Failure.Setup) {
        this.logger.add(String.format("MyDataSrc#setup %d failed", this.id));
        throw new Err("setup error");
      }
      this.logger.add(String.format("MyDataSrc#setup %d", this.id));
    }

    @Override
    public void close() {
      this.logger.add(String.format("MyDataSrc#close %d", this.id));
    }

    @Override
    public DataConn createDataConn() throws Err {
      if (this.failure == Failure.CreateDataConn) {
        this.logger.add(String.format("MyDataSrc#createDataConn %d failed", this.id));
        throw new Err("eeee");
      }
      if (this.failure == Failure.CreatedDataConnIsNull) {
        this.logger.add(String.format("MyDataSrc#createDataConn %d is null", this.id));
        return null;
      }
      this.logger.add(String.format("MyDataSrc#createDataConn %d", this.id));
      return new MyDataConn(this.id, this.failure, this.logger);
    }
  }

  static class BadDataConn implements DataConn {
    BadDataConn(int id, Failure failure, List<String> logger) {}

    @Override
    public boolean isCommitted() {
      return true;
    }

    @Override
    public void preCommit(AsyncGroup ag) throws Err {}

    @Override
    public void commit(AsyncGroup ag) throws Err {}

    @Override
    public void postCommit(AsyncGroup ag) throws Err {}

    @Override
    public void rollback(AsyncGroup ag) throws Err {}

    @Override
    public void onTxnFailure(AsyncGroup ag, List<TxnFailureReport> repots) {}

    @Override
    public void close() {}
  }

  static interface FailToCastData {}

  int countDs(List<DataSrcContainer> list) {
    int n = 0;
    for (var cont : list) {
      if (cont.ds != null) {
        n++;
      }
    }
    return n;
  }

  static void resetGlobals() {
    DataHubInner.GLOBAL_DATA_SRC_MANAGER.close();
    DataHubInner.GLOBAL_DATA_SRCS_FIXED.set(false);
  }

  ///

  @Test
  void testNewDataHubImpl() {
    var hub = new DataHubInner();
    try {
      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();
    } finally {
      hub.closeLocals();
    }
  }

  @Test
  void testNewDataHubWithCommitOrder() {
    var hub = new DataHubInner(List.of("bar", "qux", "foo"));
    try {
      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).hasSize(3);
      assertThat(hub.dataConnManager.indexMap).hasSize(3);
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();
    } finally {
      hub.closeLocals();
    }
  }

  @Test
  void testUsesAndOk() {
    var logger = new ArrayList<String>();

    var hub = new DataHubInner();
    try {
      hub.useLocal("foo", new MyDataSrc(1, Failure.None, logger));
      hub.useLocal("bar", new MyDataSrc(2, Failure.None, logger));

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(2);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

      hub.begin();

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(2);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).hasSize(2);
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isTrue();

    } catch (Exception e) {
      fail(e);
    } finally {
      hub.closeLocals();
    }
  }

  @Test
  void testUsesButAlreadyFixed() {
    var logger = new ArrayList<String>();

    var hub = new DataHubInner();
    try {
      hub.useLocal("foo", new MyDataSrc(1, Failure.None, logger));

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(1);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

      hub.begin();

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(1);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).hasSize(1);
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isTrue();

      hub.useLocal("bar", new MyDataSrc(2, Failure.None, logger));

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(1);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).hasSize(1);
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isTrue();

    } catch (Exception e) {
      fail(e);
    } finally {
      hub.closeLocals();
    }
  }

  @Test
  void testDisuseAndOk() {
    var logger = new ArrayList<String>();

    var hub = new DataHubInner();
    try {
      hub.useLocal("foo", new MyDataSrc(1, Failure.None, logger));
      hub.useLocal("bar", new MyDataSrc(2, Failure.None, logger));

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(2);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

      hub.disuseLocal("foo");

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(1);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

      hub.disuseLocal("bar");

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

    } catch (Exception e) {
      fail(e);
    } finally {
      hub.closeLocals();
    }
  }

  @Test
  void testDisuseAndFix() {
    var logger = new ArrayList<String>();

    var hub = new DataHubInner();
    try {
      hub.useLocal("foo", new MyDataSrc(1, Failure.None, logger));
      hub.useLocal("bar", new MyDataSrc(2, Failure.None, logger));

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(2);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

      hub.disuseLocal("foo");

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(1);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

      hub.disuseLocal("bar");

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

      hub.useLocal("foo", new MyDataSrc(1, Failure.None, logger));
      hub.useLocal("bar", new MyDataSrc(2, Failure.None, logger));

      hub.begin();

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(2);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).hasSize(2);
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isTrue();

      hub.disuseLocal("foo");

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(2);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).hasSize(2);
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isTrue();

      hub.disuseLocal("bar");

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(2);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).hasSize(2);
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isTrue();

      hub.end();

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(2);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).hasSize(2);
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

      hub.disuseLocal("foo");

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(1);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).hasSize(1);
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

      hub.disuseLocal("bar");

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

    } catch (Exception e) {
      fail(e);
    } finally {
      hub.closeLocals();
    }
  }

  @Test
  void testBeginIfEmpty() {
    var logger = new ArrayList<String>();

    var hub = new DataHubInner();
    try {
      hub.begin();

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isTrue();

      hub.end();

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

    } catch (Exception e) {
      fail(e);
    } finally {
      hub.closeLocals();
    }
  }

  @Test
  void testBeginAndOk() {
    var logger = new ArrayList<String>();

    var hub = new DataHubInner();
    try {
      hub.useLocal("foo", new MyDataSrc(1, Failure.None, logger));
      hub.useLocal("bar", new MyDataSrc(2, Failure.None, logger));

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(2);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

      hub.begin();

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(2);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).hasSize(2);
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isTrue();

      hub.end();

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(0);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(2);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).hasSize(2);
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

    } catch (Exception e) {
      fail(e);
    } finally {
      hub.closeLocals();
    }

    assertThat(logger).hasSize(4);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
    assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
    assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
    assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testBeginButFailed() {
    var logger = new ArrayList<String>();

    var hub = new DataHubInner();
    try {
      hub.useLocal("foo", new MyDataSrc(1, Failure.None, logger));
      hub.useLocal("bar", new MyDataSrc(2, Failure.Setup, logger));
      hub.useLocal("baz", new MyDataSrc(3, Failure.None, logger));

      assertThat(countDs(hub.localDataSrcManager.listUnready)).isEqualTo(3);
      assertThat(countDs(hub.localDataSrcManager.listReady)).isEqualTo(0);
      assertThat(hub.localDataSrcManager.local).isTrue();
      assertThat(hub.dataSrcMap).isEmpty();
      assertThat(hub.dataConnManager.list).isEmpty();
      assertThat(hub.dataConnManager.indexMap).isEmpty();
      assertThat(hub.dataConnMap).isEmpty();
      assertThat(hub.fixed).isFalse();

      try {
        hub.begin();
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToSetupLocalDataSrcs rsn -> {
            assertThat(rsn.errors()).hasSize(1);
            var ee = rsn.errors().get(0);
            assertThat(ee.index).isEqualTo(1);
            assertThat(ee.name).isEqualTo("bar");
            assertThat(ee.err.getReason()).isEqualTo("setup error");
          }
          default -> fail(err);
        }
      } finally {
        hub.end();
      }
    } catch (Exception e) {
      fail(e);
    } finally {
      hub.closeLocals();
    }

    assertThat(logger).hasSize(3);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
    assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2 failed");
    assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Nested
  class RunTest {
    @Test
    void testRunAndOk() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.None, logger));
        hub.uses("bar", new MyDataSrc(2, Failure.None, logger));

        hub.run(
            data -> {
              logger.add("execute logic");
            });

      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(5);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("execute logic");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testRunButFailedToRunLogic() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.None, logger));
        hub.uses("bar", new MyDataSrc(2, Failure.None, logger));

        hub.run(
            data -> {
              logger.add("execute logic but fail");
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case String s -> {
            assertThat(s).isEqualTo("logic error but fail");
          }
          default -> fail(err);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(5);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("execute logic but fail");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testRunButFailToCastToSpecifiedDataHub() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.None, logger));
        hub.uses("bar", new MyDataSrc(2, Failure.None, logger));

        hub.run((FailToCastData data) -> {});
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToCastDataHub r -> {
            assertThat(r.fromType()).isEqualTo(DataHub.class.getName());
          }
          default -> fail(err);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(4);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testRunButFailToSetup() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.Setup, logger));

        hub.run(
            data -> {
              logger.add("execute logic");
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToSetupLocalDataSrcs r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index).isEqualTo(0);
            assertThat(r.errors().get(0).name).isEqualTo("foo");
            assertThat(r.errors().get(0).err.toString())
                .isEqualTo(
                    "com.github.sttk.errs.Err { reason = java.lang.String setup error, file = DataHubInnerTest.java, line = 123 }");
          }
          default -> fail(err);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(1);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1 failed");
      assertThat(iter.hasNext()).isFalse();
    }
  }

  @Nested
  class TxnTest {
    @Test
    void testTxnAndNoDataAccessAndOk() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.None, logger));
        hub.uses("bar", new MyDataSrc(2, Failure.None, logger));

        hub.txn(
            data -> {
              logger.add("execute logic");
            });
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(5);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("execute logic");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testTxnAndHasDataAccessAndOk() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.None, logger));
        hub.uses("bar", new MyDataSrc(2, Failure.None, logger));

        hub.txn(
            (DataHub data) -> {
              logger.add("execute logic");

              @SuppressWarnings("unused")
              var dc1 = data.getDataConn("foo", MyDataConn.class);

              @SuppressWarnings("unused")
              var dc2 = data.getDataConn("bar", MyDataConn.class);
            });
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(15);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("execute logic");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#commit 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#commit 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#postCommit 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#postCommit 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testTxnButFailedToRunLogic() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.None, logger));
        hub.uses("bar", new MyDataSrc(2, Failure.None, logger));

        hub.txn(
            (DataHub data) -> {
              logger.add("execute logic");

              @SuppressWarnings("unused")
              var dc1 = data.getDataConn("foo", MyDataConn.class);

              @SuppressWarnings("unused")
              var dc2 = data.getDataConn("bar", MyDataConn.class);

              throw new Err("logic error");
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case String s -> {
            assertThat(s).isEqualTo("logic error");
          }
          default -> fail(err);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(13);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("execute logic");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#onTxnFailure 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#onTxnFailure 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testTxnButFailedToPreCommit() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.PreCommit, logger));
        hub.uses("bar", new MyDataSrc(2, Failure.PreCommit, logger));

        hub.txn(
            (DataHub data) -> {
              logger.add("execute logic");

              @SuppressWarnings("unused")
              var dc1 = data.getDataConn("foo", MyDataConn.class);

              @SuppressWarnings("unused")
              var dc2 = data.getDataConn("bar", MyDataConn.class);
            });

      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPreCommitDataConn r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index).isEqualTo(0);
            assertThat(r.errors().get(0).name).isEqualTo("foo");
            assertThat(r.errors().get(0).err.getReason()).isEqualTo("pre commit error");
          }
          default -> fail(err);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(14);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("execute logic");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#preCommit 1 failed");
      assertThat(iter.next()).isEqualTo("MyDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#onTxnFailure 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#onTxnFailure 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testTxnButFailedToCommit() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.Commit, logger));
        hub.uses("bar", new MyDataSrc(2, Failure.Commit, logger));

        hub.txn(
            (DataHub data) -> {
              logger.add("execute logic");

              @SuppressWarnings("unused")
              var dc1 = data.getDataConn("foo", MyDataConn.class);

              @SuppressWarnings("unused")
              var dc2 = data.getDataConn("bar", MyDataConn.class);
            });

      } catch (Err err) {
        switch (err.getReason()) {
          case FailToCommitDataConn r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index).isEqualTo(0);
            assertThat(r.errors().get(0).name).isEqualTo("foo");
            assertThat(r.errors().get(0).err.getReason()).isEqualTo("commit error");
          }
          default -> fail(err);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(16);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("execute logic");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#commit 1 failed");
      assertThat(iter.next()).isEqualTo("MyDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#onTxnFailure 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#onTxnFailure 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testTxnButFailedToPostCommit() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.PostCommit, logger));
        hub.uses("bar", new MyDataSrc(2, Failure.PostCommit, logger));

        hub.txn(
            (DataHub data) -> {
              logger.add("execute logic");

              @SuppressWarnings("unused")
              var dc1 = data.getDataConn("foo", MyDataConn.class);

              @SuppressWarnings("unused")
              var dc2 = data.getDataConn("bar", MyDataConn.class);
            });

      } catch (Err err) {
        switch (err.getReason()) {
          case FailToPostCommitDataConn r -> {
            assertThat(r.errors()).hasSize(2);
            assertThat(r.errors().get(0).index).isEqualTo(0);
            assertThat(r.errors().get(0).name).isEqualTo("foo");
            assertThat(r.errors().get(0).err.getReason()).isEqualTo("post commit error");
            assertThat(r.errors().get(1).index).isEqualTo(1);
            assertThat(r.errors().get(1).name).isEqualTo("bar");
            assertThat(r.errors().get(1).err.getReason()).isEqualTo("post commit error");
          }
          default -> fail(err);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(17);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("execute logic");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#commit 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#commit 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#postCommit 1 failed");
      assertThat(iter.next()).isEqualTo("MyDataConn#postCommit 2 failed");
      assertThat(iter.next()).isEqualTo("MyDataConn#onTxnFailure 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#onTxnFailure 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testButFailedToRollback() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.Rollback, logger));
        hub.uses("bar", new MyDataSrc(2, Failure.Rollback, logger));

        hub.txn(
            (DataHub data) -> {
              logger.add("execute logic");

              @SuppressWarnings("unused")
              var dc1 = data.getDataConn("foo", MyDataConn.class);

              @SuppressWarnings("unused")
              var dc2 = data.getDataConn("bar", MyDataConn.class);

              throw new Err("logic error");
            });

      } catch (Err err) {
        switch (err.getReason()) {
          case String s -> {
            assertThat(s).isEqualTo("logic error");
          }
          default -> fail(err);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(13);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("execute logic");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#rollback 1 failed");
      assertThat(iter.next()).isEqualTo("MyDataConn#rollback 2 failed");
      assertThat(iter.next()).isEqualTo("MyDataConn#onTxnFailure 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#onTxnFailure 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testWithCommitOrder() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub("bar", "foo")) {
        hub.uses("foo", new MyDataSrc(1, Failure.Rollback, logger));
        hub.uses("bar", new MyDataSrc(2, Failure.Rollback, logger));

        hub.txn(
            (DataHub data) -> {
              logger.add("execute logic");

              @SuppressWarnings("unused")
              var dc1 = data.getDataConn("foo", MyDataConn.class);

              @SuppressWarnings("unused")
              var dc2 = data.getDataConn("bar", MyDataConn.class);
            });

      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(15);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("execute logic");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#commit 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#commit 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#postCommit 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#postCommit 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testTxnButFailToCastToSpecifiedDataHub() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.None, logger));
        hub.uses("bar", new MyDataSrc(2, Failure.None, logger));

        hub.txn((FailToCastData data) -> {});
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToCastDataHub r -> {
            assertThat(r.fromType()).isEqualTo(DataHub.class.getName());
          }
          default -> fail(err);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(4);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testTxnButFailToSetup() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.Setup, logger));

        hub.txn(
            (FailToCastData data) -> {
              logger.add("execute logic");
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToSetupLocalDataSrcs r -> {
            assertThat(r.errors().get(0).index).isEqualTo(0);
            assertThat(r.errors().get(0).name).isEqualTo("foo");
            assertThat(r.errors().get(0).err.getReason()).isEqualTo("setup error");
          }
          default -> fail(err);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(1);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1 failed");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testGetDataConnCached() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.None, logger));

        hub.txn(
            (DataHub data) -> {
              logger.add("execute logic");

              @SuppressWarnings("unused")
              var dc1 = data.getDataConn("foo", MyDataConn.class);

              @SuppressWarnings("unused")
              var dc2 = data.getDataConn("foo", MyDataConn.class);
            });
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(8);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("execute logic");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#commit 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#postCommit 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testGetDataConnAndNoDataSrcToCreateDataConn() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.txn(
            (DataHub data) -> {
              logger.add("execute logic");

              @SuppressWarnings("unused")
              var dc1 = data.getDataConn("foo", MyDataConn.class);
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case NoDataSrcToCreateDataConn r -> {
            assertThat(r.name()).isEqualTo("foo");
            assertThat(r.dataConnType()).isEqualTo(MyDataConn.class.getName());
          }
          default -> fail(err);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(1);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("execute logic");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testGetDataConnAndCreatedDataConnIsNull() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.CreatedDataConnIsNull, logger));

        hub.txn(
            (DataHub data) -> {
              logger.add("execute logic");

              @SuppressWarnings("unused")
              var dc = data.getDataConn("foo", MyDataConn.class);
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case CreatedDataConnIsNull r -> {
            assertThat(r.name()).isEqualTo("foo");
            assertThat(r.dataConnType()).isEqualTo(MyDataConn.class.getName());
          }
          default -> fail(err);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(4);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("execute logic");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 1 is null");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testGetDataConnAndFailedToCreateDataConn() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.CreateDataConn, logger));

        hub.txn(
            (DataHub data) -> {
              logger.add("execute logic");

              @SuppressWarnings("unused")
              var dc = data.getDataConn("foo", MyDataConn.class);
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToCreateDataConn r -> {
            assertThat(r.name()).isEqualTo("foo");
            assertThat(r.dataConnType()).isEqualTo(MyDataConn.class.getName());
          }
          default -> fail(err);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(4);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("execute logic");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 1 failed");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testGetDataConnAndFailedToCastDataConn() {
      var logger = new ArrayList<String>();

      try (var hub = new DataHub()) {
        hub.uses("foo", new MyDataSrc(1, Failure.None, logger));
        hub.uses("bar", new MyDataSrc(2, Failure.None, logger));

        hub.txn(
            (DataHub data) -> {
              logger.add("execute logic");

              @SuppressWarnings("unused")
              var dc1 = data.getDataConn("foo", MyDataConn.class);

              @SuppressWarnings("unused")
              var dc2 = data.getDataConn("bar", BadDataConn.class);
            });
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToCastDataConn r -> {
            assertThat(r.name()).isEqualTo("bar");
            assertThat(r.fromDataConnType()).isEqualTo(MyDataConn.class.getName());
            assertThat(r.toDataConnType()).isEqualTo(BadDataConn.class.getName());
          }
          default -> fail(err);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(13);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("execute logic");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#createDataConn 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#rollback 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#rollback 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#onTxnFailure 1");
      assertThat(iter.next()).isEqualTo("MyDataConn#onTxnFailure 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 2");
      assertThat(iter.next()).isEqualTo("MyDataConn#close 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }
  }

  @Nested
  @SuppressWarnings("try")
  class TestGlobals {

    @Test
    void testUsesAndSetupAndOk() {
      var logger = new ArrayList<String>();
      try {
        resetGlobals();

        assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

        uses("foo", new MyDataSrc(1, Failure.None, logger));

        assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(1);
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(0);

        try (var ac = setup()) {
          assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(0);
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(1);
        }
      } catch (Exception e) {
        fail(e);
      } finally {
        resetGlobals();
      }

      assertThat(logger).hasSize(2);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testUsesAndSetupButFail() {
      var logger = new ArrayList<String>();
      try {
        resetGlobals();

        assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

        uses("foo", new MyDataSrc(1, Failure.Setup, logger));

        assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(1);
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(0);

        try (var ac = setup()) {
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case FailToSetupGlobalDataSrcs r -> {
              assertThat(r.errors()).hasSize(1);
              assertThat(r.errors().get(0).index).isEqualTo(0);
              assertThat(r.errors().get(0).name).isEqualTo("foo");
              assertThat(r.errors().get(0).err.getReason()).isEqualTo("setup error");
            }
            default -> fail(err);
          }
          assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(0);
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(0);
        }
      } catch (Exception e) {
        fail(e);
      } finally {
        resetGlobals();
      }

      assertThat(logger).hasSize(1);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1 failed");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testUsesAndSetupButAlreadyFixedBefore() {
      var logger = new ArrayList<String>();
      try {
        resetGlobals();

        assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

        try (var ac = setup()) {
          assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

          uses("foo", new MyDataSrc(1, Failure.Setup, logger));

          assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();
        }
      } catch (Exception e) {
        fail(e);
      } finally {
        resetGlobals();
      }

      assertThat(logger).hasSize(0);
    }

    @Test
    void testUsesAndSetupWithOrderAndOk() {
      var logger = new ArrayList<String>();
      try {
        resetGlobals();

        assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

        uses("foo", new MyDataSrc(1, Failure.None, logger));
        uses("bar", new MyDataSrc(2, Failure.None, logger));

        assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(2);
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(0);

        try (var ac = setup("bar", "foo")) {
          assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(0);
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(2);
        }
      } catch (Exception e) {
        fail(e);
      } finally {
        resetGlobals();
      }

      assertThat(logger).hasSize(4);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 1");
      assertThat(iter.next()).isEqualTo("MyDataSrc#close 2");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testUsesAndSetupWithOrderButFail() {
      var logger = new ArrayList<String>();
      try {
        resetGlobals();

        assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

        uses("foo", new MyDataSrc(1, Failure.Setup, logger));
        uses("bar", new MyDataSrc(2, Failure.Setup, logger));

        assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(2);
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(0);

        try (var ac = setup("bar", "foo")) {
          fail();
        } catch (Err err) {
          switch (err.getReason()) {
            case FailToSetupGlobalDataSrcs r -> {
              assertThat(r.errors()).hasSize(1);
              assertThat(r.errors().get(0).index).isEqualTo(0);
              assertThat(r.errors().get(0).name).isEqualTo("bar");
              assertThat(r.errors().get(0).err.getReason()).isEqualTo("setup error");
            }
            default -> fail(err);
          }
          assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(0);
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(0);
        }
      } catch (Exception e) {
        fail(e);
      } finally {
        resetGlobals();
      }

      assertThat(logger).hasSize(1);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("MyDataSrc#setup 2 failed");
      assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void testUsesAndSetupWithOrderButAlreadyFixedBefore() {
      var logger = new ArrayList<String>();
      try {
        resetGlobals();

        assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
        assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

        try (var ac = setup("bar", "foo")) {
          assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

          uses("foo", new MyDataSrc(1, Failure.Setup, logger));

          assertThat(DataHubInner.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
          assertThat(DataHubInner.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();
        }
      } catch (Exception e) {
        fail(e);
      } finally {
        resetGlobals();
      }
    }
  }
}
