package com.github.sttk.sabi.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.DataSrc;
import com.github.sttk.sabi.DataAcc;
import com.github.sttk.sabi.DataHub;
import com.github.sttk.sabi.Sabi;
import com.github.sttk.sabi.Logic;
import java.util.List;
import java.util.ArrayList;

public class DataAccTest {
  DataAccTest() {}

  final void suppressWarnings_unused(Object a) {}

  static class FooDataSrc implements DataSrc {
    private int id;
    private String text;
    private List<String> logger;
    private boolean willFail;

    FooDataSrc(int id, String text, List<String> logger, boolean willFail) {
      this.id = id;
      this.text = text;
      this.logger = logger;
      this.willFail = willFail;
    }
    @Override
    public void setup(AsyncGroup ag) throws Exc {
      if (this.willFail) {
        this.logger.add(String.format("FooDataSrc %d failed to setup", this.id));
        throw new Exc("XXX");
      }
      this.logger.add(String.format("FooDataSrc %d setupped", this.id));
    }
    @Override
    public void close() {
      this.logger.add(String.format("FooDataSrc %d closed", this.id));
    }
    @Override
    public DataConn createDataConn() throws Exc {
      this.logger.add(String.format("FooDataSrc %d created FooDataConn", this.id));
      return new FooDataConn(this.id, this.text, this.logger);
    }
  }

  static class FooDataConn implements DataConn {
    private int id;
    private String text;
    private boolean committed;
    private List<String> logger;

    FooDataConn(int id, String text, List<String> logger) {
      this.id = id;
      this.text = text;
      this.logger = logger;
    }
    String getText() {
      return this.text;
    }
    @Override
    public void commit(AsyncGroup ag) throws Exc {
      this.committed = true;
      this.logger.add(String.format("FooDataConn %d committed", this.id));
    }
    @Override
    public void preCommit(AsyncGroup ag) throws Exc {
      this.logger.add(String.format("FooDataConn %d pre committed", this.id));
    }
    @Override
    public void postCommit(AsyncGroup ag) {
      this.logger.add(String.format("FooDataConn %d post committed", this.id));
    }
    @Override
    public boolean shouldForceBack() {
      return this.committed;
    }
    @Override
    public void rollback(AsyncGroup ag) {
      this.logger.add(String.format("FooDataConn %d rollbacked", this.id));
    }
    @Override
    public void forceBack(AsyncGroup ag) {
      this.logger.add(String.format("FooDataConn %d forced back", this.id));
    }
    @Override
    public void close() {
      this.logger.add(String.format("FooDataConn %d closed", this.id));
    }
  }

  static class BarDataSrc implements DataSrc {
    private int id;
    private String text;
    private List<String> logger;
    private boolean willFail;

    BarDataSrc(int id, List<String> logger, boolean willFail) {
      this.id = id;
      this.text = null;
      this.logger = logger;
      this.willFail = willFail;
    }
    @Override
    public void setup(AsyncGroup ag) throws Exc {
      if (this.willFail) {
        this.logger.add(String.format("BarDataSrc %d failed to setup", this.id));
        throw new Exc("XXX");
      }
      this.logger.add(String.format("BarDataSrc %d setupped", this.id));
    }
    @Override
    public void close() {
      this.logger.add(String.format("BarDataSrc.text = %s", this.text));
      this.logger.add(String.format("BarDataSrc %d closed", this.id));
    }
    @Override
    public DataConn createDataConn() throws Exc {
      this.logger.add(String.format("BarDataSrc %d created BarDataConn", this.id));
      return new BarDataConn(this.id, this.text, this.logger, this);
    }
  }

  static class BarDataConn implements DataConn {
    private int id;
    private String text;
    private boolean committed;
    private List<String> logger;
    private BarDataSrc ds;

    BarDataConn(int id, String text, List<String> logger, BarDataSrc ds) {
      this.id = id;
      this.text = text;
      this.committed = false;
      this.logger = logger;
      this.ds = ds;
    }
    void setText(String s) {
      this.text = s;
    }
    @Override
    public void commit(AsyncGroup ag) throws Exc {
      this.committed = true;
      this.ds.text = this.text;
      this.logger.add(String.format("BarDataConn %d committed", this.id));
    }
    @Override
    public void preCommit(AsyncGroup ag) throws Exc {
      this.logger.add(String.format("BarDataConn %d pre committed", this.id));
    }
    @Override
    public void postCommit(AsyncGroup ag) {
      this.logger.add(String.format("BarDataConn %d post committed", this.id));
    }
    @Override
    public boolean shouldForceBack() {
      return this.committed;
    }
    @Override
    public void rollback(AsyncGroup ag) {
      this.logger.add(String.format("BarDataConn %d rollbacked", this.id));
    }
    @Override
    public void forceBack(AsyncGroup ag) {
      this.logger.add(String.format("BarDataConn %d forced back", this.id));
    }
    @Override
    public void close() {
      this.logger.add(String.format("BarDataConn.text = %s", this.text));
      this.logger.add(String.format("BarDataConn %d closed", this.id));
    }
  }

  ///

  static interface SampleData {
    String getValue() throws Exc;
    void setValue(String text) throws Exc;
  }

  static class SampleLogic implements Logic<SampleData> {
    @Override
    public void run(SampleData data) throws Exc {
      var v = data.getValue();
      data.setValue(v);
    }
  }

  static class FailingLogic implements Logic<SampleData> {
    @Override
    public void run(SampleData data) throws Exc {
      throw new Exc("ZZZ");
    }
  }

  static interface AllLogicData extends SampleData {}

  static interface FooDataAcc extends DataAcc, AllLogicData {
    @Override
    default String getValue() throws Exc {
      var conn = getDataConn("foo", FooDataConn.class);
      return conn.getText();
    }
  }

  static interface BarDataAcc extends DataAcc, AllLogicData {
    @Override
    default void setValue(String text) throws Exc {
      var conn = getDataConn("bar", BarDataConn.class);
      conn.setText(text);
    }
  }

  ///

  static class SampleDataHub extends DataHub implements FooDataAcc, BarDataAcc {}

  ///

  @Nested
  class TestLogicArgument {
    @BeforeEach
    void beforeEach() {
      DataHubInnerTest.resetGlobalVariables();
    }
    @AfterEach
    void afterEach() {
      DataHubInnerTest.resetGlobalVariables();
    }

    @Test
    void test() {
      var logger = new ArrayList<String>();

      Sabi.uses("foo", new FooDataSrc(1, "hello", logger, false));
      Sabi.uses("bar", new BarDataSrc(2, logger, false));

      try (var ac = Sabi.setup()) {
        suppressWarnings_unused(ac);
        try (var hub = new SampleDataHub()) {
          new SampleLogic().run(hub);
        } catch (Exception e) {
          fail(e);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).containsExactly(
        "FooDataSrc 1 setupped",
        "BarDataSrc 2 setupped",
        "FooDataSrc 1 created FooDataConn",
        "BarDataSrc 2 created BarDataConn",
        "BarDataConn.text = hello",
        "BarDataConn 2 closed",
        "FooDataConn 1 closed",
        "BarDataSrc.text = null",
        "BarDataSrc 2 closed",
        "FooDataSrc 1 closed"
      );
    }
  }

  @Nested
  class TestDataHubRunUsingGlobal {
    @BeforeEach
    void beforeEach() {
      DataHubInnerTest.resetGlobalVariables();
    }
    @AfterEach
    void afterEach() {
      DataHubInnerTest.resetGlobalVariables();
    }

    @Test
    void test() {
      var logger = new ArrayList<String>();

      Sabi.uses("foo", new FooDataSrc(1, "hello", logger, false));
      Sabi.uses("bar", new BarDataSrc(2, logger, false));

      try (var ac = Sabi.setup()) {
        suppressWarnings_unused(ac);
        try (var hub = new SampleDataHub()) {
          hub.run(new SampleLogic());
        } catch (Exception e) {
          fail(e);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).containsExactly(
        "FooDataSrc 1 setupped",
        "BarDataSrc 2 setupped",
        "FooDataSrc 1 created FooDataConn",
        "BarDataSrc 2 created BarDataConn",
        "BarDataConn.text = hello",
        "BarDataConn 2 closed",
        "FooDataConn 1 closed",
        "BarDataSrc.text = null",
        "BarDataSrc 2 closed",
        "FooDataSrc 1 closed"
      );
    }
  }

  @Nested
  class TestDataHubRunUsingLocal {
    @BeforeEach
    void beforeEach() {
      DataHubInnerTest.resetGlobalVariables();
    }
    @AfterEach
    void afterEach() {
      DataHubInnerTest.resetGlobalVariables();
    }

    @Test
    void test() {
      var logger = new ArrayList<String>();

      try (var ac = Sabi.setup()) {
        suppressWarnings_unused(ac);
        try (var hub = new SampleDataHub()) {
          hub.uses("foo", new FooDataSrc(1, "hello", logger, false));
          hub.uses("bar", new BarDataSrc(2, logger, false));

          hub.run(new SampleLogic());
        } catch (Exception e) {
          fail(e);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).containsExactly(
        "FooDataSrc 1 setupped",
        "BarDataSrc 2 setupped",
        "FooDataSrc 1 created FooDataConn",
        "BarDataSrc 2 created BarDataConn",
        "BarDataConn.text = hello",
        "BarDataConn 2 closed",
        "FooDataConn 1 closed",
        "BarDataSrc.text = null",
        "BarDataSrc 2 closed",
        "FooDataSrc 1 closed"
      );
    }

    @Test
    void test_not_run_logic_if_fail_to_setup_local_data_src() {
      var logger = new ArrayList<String>();

      try (var ac = Sabi.setup()) {
        suppressWarnings_unused(ac);
        try (var hub = new SampleDataHub()) {
          hub.uses("foo", new FooDataSrc(1, "hello", logger, true));
          hub.uses("bar", new BarDataSrc(2, logger, false));

          hub.run(new SampleLogic());
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToSetupLocalDataSrcs r -> {
              var e2 = r.errors().get("foo");
              assertThat(e2.getReason()).isEqualTo("XXX");
            }
            default -> fail(e);
          }
        } catch (Exception e) {
          fail(e);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).containsExactly(
        "FooDataSrc 1 failed to setup"
      );
    }
  }

  @Nested
  class TestDataHubRunUsingGlobalAndLocal {
    @BeforeEach
    void beforeEach() {
      DataHubInnerTest.resetGlobalVariables();
    }
    @AfterEach
    void afterEach() {
      DataHubInnerTest.resetGlobalVariables();
    }

    @Test
    void test() {
      var logger = new ArrayList<String>();

      Sabi.uses("bar", new BarDataSrc(1, logger, false));

      try (var ac = Sabi.setup()) {
        suppressWarnings_unused(ac);
        try (var hub = new SampleDataHub()) {
          hub.uses("foo", new FooDataSrc(2, "Hello", logger, false));

          hub.run(new SampleLogic());
        } catch (Exception e) {
          fail(e);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).containsExactly(
        "BarDataSrc 1 setupped",
        "FooDataSrc 2 setupped",
        "FooDataSrc 2 created FooDataConn",
        "BarDataSrc 1 created BarDataConn",
        "BarDataConn.text = Hello",
        "BarDataConn 1 closed",
        "FooDataConn 2 closed",
        "FooDataSrc 2 closed",
        "BarDataSrc.text = null",
        "BarDataSrc 1 closed"
      );
    }
  }

  @Nested
  class TestDataHubTxnUsingGlobal {
    @BeforeEach
    void beforeEach() {
      DataHubInnerTest.resetGlobalVariables();
    }
    @AfterEach
    void afterEach() {
      DataHubInnerTest.resetGlobalVariables();
    }

    @Test
    void test() {
      var logger = new ArrayList<String>();

      Sabi.uses("foo", new FooDataSrc(1, "Hello", logger, false));
      Sabi.uses("bar", new BarDataSrc(2, logger, false));

      try (var ac = Sabi.setup()) {
        suppressWarnings_unused(ac);
        try (var hub = new SampleDataHub()) {
          hub.txn(new SampleLogic());
        } catch (Exception e) {
          fail(e);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).containsExactly(
        "FooDataSrc 1 setupped",
        "BarDataSrc 2 setupped",
        "FooDataSrc 1 created FooDataConn",
        "BarDataSrc 2 created BarDataConn",
        "FooDataConn 1 pre committed",
        "BarDataConn 2 pre committed",
        "FooDataConn 1 committed",
        "BarDataConn 2 committed",
        "FooDataConn 1 post committed",
        "BarDataConn 2 post committed",
        "BarDataConn.text = Hello",
        "BarDataConn 2 closed",
        "FooDataConn 1 closed",
        "BarDataSrc.text = Hello",
        "BarDataSrc 2 closed",
        "FooDataSrc 1 closed"
      );
    }
  }

  @Nested
  class TestDataHubTxnUsingLocal {
    @BeforeEach
    void beforeEach() {
      DataHubInnerTest.resetGlobalVariables();
    }
    @AfterEach
    void afterEach() {
      DataHubInnerTest.resetGlobalVariables();
    }

    @Test
    void test() {
      var logger = new ArrayList<String>();

      try (var ac = Sabi.setup()) {
        suppressWarnings_unused(ac);
        try (var hub = new SampleDataHub()) {
          hub.uses("foo", new FooDataSrc(1, "Hello", logger, false));
          hub.uses("bar", new BarDataSrc(2, logger, false));

          hub.txn(new SampleLogic());
        } catch (Exception e) {
          fail(e);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).containsExactly(
        "FooDataSrc 1 setupped",
        "BarDataSrc 2 setupped",
        "FooDataSrc 1 created FooDataConn",
        "BarDataSrc 2 created BarDataConn",
        "FooDataConn 1 pre committed",
        "BarDataConn 2 pre committed",
        "FooDataConn 1 committed",
        "BarDataConn 2 committed",
        "FooDataConn 1 post committed",
        "BarDataConn 2 post committed",
        "BarDataConn.text = Hello",
        "BarDataConn 2 closed",
        "FooDataConn 1 closed",
        "BarDataSrc.text = Hello",
        "BarDataSrc 2 closed",
        "FooDataSrc 1 closed"
      );
    }

    @Test
    void test_not_run_logic_if_fail_to_setup_local_data_src() {
      var logger = new ArrayList<String>();

      try (var ac = Sabi.setup()) {
        suppressWarnings_unused(ac);
        try (var hub = new SampleDataHub()) {
          hub.uses("foo", new FooDataSrc(1, "Hello", logger, true));
          hub.uses("bar", new BarDataSrc(2, logger, false));

          hub.txn(new SampleLogic());
        } catch (Exc e) {
          switch (e.getReason()) {
            case DataHub.FailToSetupLocalDataSrcs r -> {
              var e2 = r.errors().get("foo");
              assertThat(e2.getReason()).isEqualTo("XXX");
            }
            default -> fail(e);
          }
        } catch (Exception e) {
          fail(e);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).containsExactly(
        "FooDataSrc 1 failed to setup"
      );
    }

    @Test
    void test_not_run_logic_in_txn_and_rollback() {
      var logger = new ArrayList<String>();

      try (var ac = Sabi.setup()) {
        suppressWarnings_unused(ac);
        try (var hub = new SampleDataHub()) {
          hub.uses("foo", new FooDataSrc(1, "Hello", logger, false));
          hub.uses("bar", new BarDataSrc(2, logger, false));

          hub.txn(new FailingLogic());
        } catch (Exc e) {
          assertThat(e.getReason()).isEqualTo("ZZZ");
        } catch (Exception e) {
          fail(e);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).containsExactly(
        "FooDataSrc 1 setupped",
        "BarDataSrc 2 setupped",
        "BarDataSrc.text = null",
        "BarDataSrc 2 closed",
        "FooDataSrc 1 closed"
      );
    }
  }

  @Nested
  class TestDataHubTxnUsingGlobalAndLocal {
    @BeforeEach
    void beforeEach() {
      DataHubInnerTest.resetGlobalVariables();
    }
    @AfterEach
    void afterEach() {
      DataHubInnerTest.resetGlobalVariables();
    }

    @Test
    void test() {
      var logger = new ArrayList<String>();

      Sabi.uses("bar", new BarDataSrc(1, logger, false));

      try (var ac = Sabi.setup()) {
        suppressWarnings_unused(ac);
        try (var hub = new SampleDataHub()) {
          hub.uses("foo", new FooDataSrc(2, "Hello", logger, false));

          hub.txn(new SampleLogic());
        } catch (Exception e) {
          fail(e);
        }
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).containsExactly(
        "BarDataSrc 1 setupped",
        "FooDataSrc 2 setupped",
        "FooDataSrc 2 created FooDataConn",
        "BarDataSrc 1 created BarDataConn",
        "FooDataConn 2 pre committed",
        "BarDataConn 1 pre committed",
        "FooDataConn 2 committed",
        "BarDataConn 1 committed",
        "FooDataConn 2 post committed",
        "BarDataConn 1 post committed",
        "BarDataConn.text = Hello",
        "BarDataConn 1 closed",
        "FooDataConn 2 closed",
        "FooDataSrc 2 closed",
        "BarDataSrc.text = Hello",
        "BarDataSrc 1 closed"
      );
    }
  }
}
