package com.github.sttk.sabi.internal;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.DataSrc;
import com.github.sttk.sabi.TxnFailureReport;
import java.util.List;

public class TestCommonsTest {
  private TestCommonsTest() {}

  static enum Fail {
    None,
    Commit,
    PreCommit,
    PostCommit,
    Rollback,
    PreCommitBecomeCommitted,
    CommitByRuntimeException,
    PreCommitByRuntimeException,
    PostCommitByRuntimeException,
    RollbackByRuntimeException,
    Setup,
    SetupByRuntimeException,
    CreateDataConn,
  }

  static class SyncDataConn implements DataConn {
    int id;
    boolean committed;
    Fail fail;
    List<String> logger;

    SyncDataConn(int id, List<String> logger, Fail fail) {
      this.id = id;
      this.committed = false;
      this.fail = fail;
      this.logger = logger;
    }

    @Override
    public void commit(AsyncGroup ag) throws Err {
      if (this.fail == Fail.Commit) {
        this.logger.add(String.format("SyncDataConn#commit %d failed", this.id));
        throw new Err("ZZZ");
      }
      if (this.fail == Fail.CommitByRuntimeException) {
        this.logger.add(String.format("SyncDataConn#commit %d runtime error", this.id));
        throw new RuntimeException("R");
      }
      this.committed = true;
      this.logger.add(String.format("SyncDataConn#commit %d", this.id));
    }

    @Override
    public void preCommit(AsyncGroup ag) throws Err {
      if (this.fail == Fail.PreCommit) {
        this.logger.add(String.format("SyncDataConn#preCommit %d failed", this.id));
        throw new Err("zzz");
      }
      if (this.fail == Fail.PreCommitByRuntimeException) {
        this.logger.add(String.format("SyncDataConn#preCommit %d runtime error", this.id));
        throw new RuntimeException("RR");
      }
      this.logger.add(String.format("SyncDataConn#preCommit %d", this.id));
      if (this.fail == Fail.PreCommitBecomeCommitted) {
        this.committed = true;
      }
    }

    @Override
    public void postCommit(AsyncGroup ag) throws Err {
      if (this.fail == Fail.PostCommit) {
        this.logger.add(String.format("SyncDataConn#postCommit %d failed", this.id));
        throw new Err("!!!");
      }
      if (this.fail == Fail.PostCommitByRuntimeException) {
        this.logger.add(String.format("SyncDataConn#postCommit %d runtime error", this.id));
        throw new RuntimeException("RRR");
      }
      this.logger.add(String.format("SyncDataConn#postCommit %d", this.id));
    }

    @Override
    public boolean isCommitted() {
      return this.committed;
    }

    @Override
    public void rollback(AsyncGroup ag) throws Err {
      if (this.fail == Fail.Rollback) {
        this.logger.add(String.format("SyncDataConn#rollback %d failed", this.id));
        throw new Err("???");
      }
      if (this.fail == Fail.RollbackByRuntimeException) {
        this.logger.add(String.format("SyncDataConn#rollback %d runtime error", this.id));
        throw new RuntimeException("RRRR");
      }
      this.logger.add(String.format("SyncDataConn#rollback %d", this.id));
    }

    @Override
    public void onTxnFailure(AsyncGroup ag, List<TxnFailureReport> reports) {
      this.logger.add(String.format("SyncDataConn#onTxnFailure %d", this.id));
      this.logger.add(String.format("TxnFailureReports=%s", reports.toString()));
    }

    @Override
    public void close() {
      this.logger.add(String.format("SyncDataConn#close %d", this.id));
    }
  }

  static class AsyncDataConn implements DataConn {
    int id;
    boolean committed;
    Fail fail;
    List<String> logger;

    AsyncDataConn(int id, List<String> logger, Fail fail) {
      this.id = id;
      this.committed = false;
      this.fail = fail;
      this.logger = logger;
    }

    @Override
    public void commit(AsyncGroup ag) throws Err {
      ag.add(
          () -> {
            try {
              Thread.sleep(10);
            } catch (Exception e) {
            }
            if (this.fail == Fail.Commit) {
              this.logger.add(String.format("AsyncDataConn#commit %d failed", this.id));
              throw new Err("YYY");
            }
            if (this.fail == Fail.CommitByRuntimeException) {
              this.logger.add(String.format("AsyncDataConn#commit %d runtime error", this.id));
              throw new RuntimeException("r");
            }
            this.committed = true;
            this.logger.add(String.format("AsyncDataConn#commit %d", this.id));
          });
    }

    @Override
    public void preCommit(AsyncGroup ag) throws Err {
      ag.add(
          () -> {
            try {
              Thread.sleep(10);
            } catch (Exception e) {
            }
            if (this.fail == Fail.PreCommit) {
              this.logger.add(String.format("AsyncDataConn#preCommit %d failed", this.id));
              throw new Err("yyy");
            }
            if (this.fail == Fail.PreCommitByRuntimeException) {
              this.logger.add(String.format("AsyncDataConn#preCommit %d runtime error", this.id));
              throw new RuntimeException("rr");
            }
            this.logger.add(String.format("AsyncDataConn#preCommit %d", this.id));
            if (this.fail == Fail.PreCommitBecomeCommitted) {
              this.committed = true;
            }
          });
    }

    @Override
    public void postCommit(AsyncGroup ag) throws Err {
      ag.add(
          () -> {
            try {
              Thread.sleep(10);
            } catch (Exception e) {
            }
            if (this.fail == Fail.PostCommit) {
              this.logger.add(String.format("AsyncDataConn#postCommit %d failed", this.id));
              throw new Err("!!!");
            }
            if (this.fail == Fail.PostCommitByRuntimeException) {
              this.logger.add(String.format("AsyncDataConn#postCommit %d runtime error", this.id));
              throw new RuntimeException("rrr");
            }
            this.logger.add(String.format("AsyncDataConn#postCommit %d", this.id));
          });
    }

    @Override
    public boolean isCommitted() {
      return this.committed;
    }

    @Override
    public void rollback(AsyncGroup ag) throws Err {
      ag.add(
          () -> {
            try {
              Thread.sleep(10);
            } catch (Exception e) {
            }
            if (this.fail == Fail.Rollback) {
              this.logger.add(String.format("AsyncDataConn#rollback %d failed", this.id));
              throw new Err("???");
            }
            if (this.fail == Fail.RollbackByRuntimeException) {
              this.logger.add(String.format("AsyncDataConn#rollback %d runtime error", this.id));
              throw new RuntimeException("rrrr");
            }
            this.logger.add(String.format("AsyncDataConn#rollback %d", this.id));
          });
    }

    @Override
    public void onTxnFailure(AsyncGroup ag, List<TxnFailureReport> reports) {
      ag.add(
          () -> {
            try {
              Thread.sleep(10);
            } catch (Exception e) {
            }
            this.logger.add(String.format("AsyncDataConn#onTxnFailure %d", this.id));
            this.logger.add(String.format("TxnFailureReports=%s", reports.toString()));
          });
    }

    @Override
    public void close() {
      this.logger.add(String.format("AsyncDataConn#close %d", this.id));
    }
  }

  static class SyncDataSrc implements DataSrc {
    int id;
    Fail fail;
    List<String> logger;

    SyncDataSrc(int id, List<String> logger, Fail fail) {
      logger.add(String.format("SyncDataSrc#new %d", id));
      this.id = id;
      this.logger = logger;
      this.fail = fail;
    }

    @Override
    public void setup(AsyncGroup ag) throws Err {
      if (this.fail == Fail.Setup) {
        this.logger.add(String.format("SyncDataSrc#setup %d failed", this.id));
        throw new Err("XXX");
      }
      if (this.fail == Fail.SetupByRuntimeException) {
        this.logger.add(String.format("SyncDataSrc#setup %d runtime error", this.id));
        throw new RuntimeException();
      }
      this.logger.add(String.format("SyncDataSrc#setup %d", this.id));
    }

    @Override
    public void close() {
      this.logger.add(String.format("SyncDataSrc#close %d", this.id));
    }

    @Override
    public DataConn createDataConn() throws Err {
      if (this.fail == Fail.CreateDataConn) {
        this.logger.add(String.format("SyncDataSrc#createDataConn %d failed", this.id));
        throw new Err("XXX");
      }
      this.logger.add(String.format("SyncDataSrc#createDataConn %d", this.id));
      return new SyncDataConn(this.id, this.logger, Fail.None);
    }
  }

  static class AsyncDataSrc implements DataSrc {
    int id;
    List<String> logger;
    Fail fail;

    AsyncDataSrc(int id, List<String> logger, Fail fail) {
      logger.add(String.format("AsyncDataSrc#new %d", id));
      this.id = id;
      this.logger = logger;
      this.fail = fail;
    }

    @Override
    public void setup(AsyncGroup ag) throws Err {
      ag.add(
          () -> {
            if (this.fail == Fail.Setup) {
              this.logger.add(String.format("AsyncDataSrc#setup %d failed", this.id));
              throw new Err("XXX");
            }
            if (this.fail == Fail.SetupByRuntimeException) {
              this.logger.add(String.format("AsyncDataSrc#setup %d runtime error", this.id));
              throw new RuntimeException();
            }
            this.logger.add(String.format("AsyncDataSrc#setup %d", this.id));
          });
    }

    @Override
    public void close() {
      this.logger.add(String.format("AsyncDataSrc#close %d", this.id));
    }

    @Override
    public DataConn createDataConn() throws Err {
      if (this.fail == Fail.CreateDataConn) {
        this.logger.add(String.format("AsyncDataSrc#createDataConn %d failed", this.id));
        throw new Err("XXX");
      }
      this.logger.add(String.format("AsyncDataSrc#createDataConn %d", this.id));
      return new AsyncDataConn(this.id, this.logger, Fail.None);
    }
  }
}
