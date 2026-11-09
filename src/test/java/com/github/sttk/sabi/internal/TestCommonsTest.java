package com.github.sttk.sabi.internal;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.DataSrc;
import com.github.sttk.sabi.TxnFailureReport;
import java.util.List;

class TestCommonsTest {
  private TestCommonsTest() {}

  static enum Fail {
    None,
    Setup,
    SetupFatal,
    CreateDataConn,

    PreCommit,
    Commit,
    PostCommit,
    Rollback,

    PreCommitError,
    CommitError,
    PostCommitError,
    RollbackError,
    OnTxnFailureError,
    CloseError,

    PreCommitRuntimeException,
    CommitRuntimeException,
    PostCommitRuntimeException,
    RollbackRuntimeException,
    OnTxnFailureRuntimeException,
    CloseRuntimeException,

    PreCommitBecomeCommitted,
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
      } else if (this.fail == Fail.CommitError) {
        this.logger.add(String.format("SyncDataConn#commit %d Error", this.id));
        throw new Error("commit Error");
      } else if (this.fail == Fail.CommitRuntimeException) {
        this.logger.add(String.format("SyncDataConn#commit %d RuntimeException", this.id));
        throw new RuntimeException("commit RuntimeException");
      }
      this.committed = true;
      this.logger.add(String.format("SyncDataConn#commit %d", this.id));
    }

    @Override
    public void preCommit(AsyncGroup ag) throws Err {
      if (this.fail == Fail.PreCommit) {
        this.logger.add(String.format("SyncDataConn#preCommit %d failed", this.id));
        throw new Err("zzz");
      } else if (this.fail == Fail.PreCommitError) {
        this.logger.add(String.format("SyncDataConn#preCommit %d Error", this.id));
        throw new Error("preCommit Error");
      } else if (this.fail == Fail.PreCommitRuntimeException) {
        this.logger.add(String.format("SyncDataConn#preCommit %d RuntimeException", this.id));
        throw new RuntimeException("preCommit RuntimeException");
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
      } else if (this.fail == Fail.PostCommitError) {
        this.logger.add(String.format("SyncDataConn#postCommit %d Error", this.id));
        throw new Error("postCommit Error");
      } else if (this.fail == Fail.PostCommitRuntimeException) {
        this.logger.add(String.format("SyncDataConn#postCommit %d RuntimeException", this.id));
        throw new RuntimeException("postCommit RuntimeException");
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
      } else if (this.fail == Fail.RollbackError) {
        this.logger.add(String.format("SyncDataConn#rollback %d Error", this.id));
        throw new Error("rollback Error");
      } else if (this.fail == Fail.RollbackRuntimeException) {
        this.logger.add(String.format("SyncDataConn#rollback %d RuntimeException", this.id));
        throw new RuntimeException("rollback RuntimeException");
      }
      this.logger.add(String.format("SyncDataConn#rollback %d", this.id));
    }

    @Override
    public void onTxnFailure(AsyncGroup ag, List<TxnFailureReport> reports) {
      if (this.fail == Fail.OnTxnFailureError) {
        this.logger.add(String.format("SyncDataConn#onTxnFailure %d Error", this.id));
        throw new Error("onTxnFailure Error");
      } else if (this.fail == Fail.OnTxnFailureRuntimeException) {
        this.logger.add(String.format("SyncDataConn#onTxnFailure %d RuntimeException", this.id));
        throw new RuntimeException("onTxnFailure RuntimeException");
      }
      this.logger.add(String.format("SyncDataConn#onTxnFailure %d", this.id));
      this.logger.add(String.format("TxnFailureReports=%s", reports.toString()));
    }

    @Override
    public void close() {
      if (this.fail == Fail.CloseError) {
        this.logger.add(String.format("SyncDataConn#close %d Error", this.id));
        throw new Error("close Error");
      } else if (this.fail == Fail.CloseRuntimeException) {
        this.logger.add(String.format("SyncDataConn#close %d RuntimeException", this.id));
        throw new RuntimeException("close RuntimeException");
      }
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
              Thread.sleep(200);
            } catch (Exception e) {
            }
            if (this.fail == Fail.Commit) {
              this.logger.add(String.format("AsyncDataConn#commit %d failed", this.id));
              throw new Err("YYY");
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
              Thread.sleep(200);
            } catch (Exception e) {
            }
            if (this.fail == Fail.PreCommit) {
              this.logger.add(String.format("AsyncDataConn#preCommit %d failed", this.id));
              throw new Err("yyy");
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
              Thread.sleep(200);
            } catch (Exception e) {
            }
            if (this.fail == Fail.PostCommit) {
              this.logger.add(String.format("AsyncDataConn#postCommit %d failed", this.id));
              throw new Err("!!!");
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
              Thread.sleep(200);
            } catch (Exception e) {
            }
            if (this.fail == Fail.Rollback) {
              this.logger.add(String.format("AsyncDataConn#rollback %d failed", this.id));
              throw new Err("yyy");
            }
            this.logger.add(String.format("AsyncDataConn#rollback %d", this.id));
          });
    }

    @Override
    public void onTxnFailure(AsyncGroup ag, List<TxnFailureReport> reports) {
      ag.add(
          () -> {
            try {
              Thread.sleep(200);
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

  static class NoCommitDataConn implements DataConn {
    int id;
    List<String> logger;

    NoCommitDataConn(int id, List<String> logger) {
      this.id = id;
      this.logger = logger;
    }

    @Override
    public void commit(AsyncGroup ag) throws Err {
      this.logger.add(String.format("NoCommitDataConn#commit %d", this.id));
    }

    @Override
    public void preCommit(AsyncGroup ag) throws Err {
      this.logger.add(String.format("NoCommitDataConn#preCommit %d", this.id));
    }

    @Override
    public void postCommit(AsyncGroup ag) throws Err {
      this.logger.add(String.format("NoCommitDataConn#postCommit %d", this.id));
    }

    @Override
    public boolean isCommitted() {
      return false;
    }

    @Override
    public void rollback(AsyncGroup ag) throws Err {
      this.logger.add(String.format("NoCommitDataConn#rollback %d", this.id));
    }

    @Override
    public void onTxnFailure(AsyncGroup ag, List<TxnFailureReport> reports) {
      this.logger.add(String.format("NoCommitDataConn#onTxnFailure %d", this.id));
      this.logger.add(String.format("TxnFailureReports=%s", reports.toString()));
    }

    @Override
    public void close() {
      this.logger.add(String.format("NoCommitDataConn#close %d", this.id));
    }
  }

  static class SyncDataSrc implements DataSrc {
    int id;
    List<String> logger;
    Fail fail;

    SyncDataSrc(int id, List<String> logger, Fail fail) {
      logger.add(String.format("SyncDataSrc#new %d", id));
      this.id = id;
      this.logger = logger;
      this.fail = fail;
    }

    @Override
    public void setup(AsyncGroup ag) throws Err {
      if (this.fail == Fail.Setup) {
        this.logger.add(String.format("SyncDataConn#setup %d failed", this.id));
        throw new Err("XXX");
      } else if (this.fail == Fail.SetupFatal) {
        this.logger.add(String.format("SyncDataConn#setup %d fatal", this.id));
        throw new Error("setup fatal");
      }
      this.logger.add(String.format("SyncDataSrc#setup %d", this.id));
    }

    @Override
    public void close() {
      this.logger.add(String.format("SyncDataConn#close %d", this.id));
    }

    @Override
    public DataConn createDataConn() throws Err {
      if (this.fail == Fail.CreateDataConn) {
        this.logger.add(String.format("SyncDataConn#createDataConn %d failed", this.id));
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
              this.logger.add(String.format("AsyncDataConn#setup %d failed", this.id));
              throw new Err("XXX");
            } else if (this.fail == Fail.SetupFatal) {
              this.logger.add(String.format("AsyncDataConn#setup %d fatal", this.id));
              throw new Error("setup fatal");
            }
            this.logger.add(String.format("AsyncDataSrc#setup %d", this.id));
          });
    }

    @Override
    public void close() {
      this.logger.add(String.format("AsyncDataConn#close %d", this.id));
    }

    @Override
    public DataConn createDataConn() throws Err {
      if (this.fail == Fail.CreateDataConn) {
        this.logger.add(String.format("AsyncDataConn#createDataConn %d failed", this.id));
        throw new Err("XXX");
      }
      this.logger.add(String.format("AsyncDataSrc#createDataConn %d", this.id));
      return new AsyncDataConn(this.id, this.logger, Fail.None);
    }
  }
}
