package com.github.sttk.sabi.internal;

import static com.github.sttk.sabi.Sabi.setup;
import static com.github.sttk.sabi.Sabi.uses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.DataAcc;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.DataHub;
import com.github.sttk.sabi.DataSrc;
import com.github.sttk.sabi.Logic;
import com.github.sttk.sabi.TxnFailureReport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DataAccTest {
  private DataAccTest() {}

  static class FooDataConn implements DataConn {
    final int id;
    final String text;
    boolean committed;
    List<String> logger;

    FooDataConn(int id, String s, List<String> logger) {
      this.id = id;
      this.text = s;
      this.logger = logger;
      logger.add(String.format("FooDataConn#new %d", id));
    }

    public String getText() {
      this.logger.add(String.format("FooDataConn#getText %d", this.id));
      return this.text;
    }

    @Override
    public void commit(AsyncGroup ag) throws Err {
      this.committed = true;
      this.logger.add(String.format("FooDataConn#commit %d", this.id));
    }

    @Override
    public void preCommit(AsyncGroup ag) throws Err {
      this.logger.add(String.format("FooDataConn#preCommit %d", this.id));
    }

    @Override
    public void postCommit(AsyncGroup ag) throws Err {
      this.logger.add(String.format("FooDataConn#postCommit %d", this.id));
    }

    @Override
    public boolean isCommitted() {
      return this.committed;
    }

    @Override
    public void rollback(AsyncGroup ag) throws Err {
      this.logger.add(String.format("FooDataConn#rollback %d", this.id));
    }

    @Override
    public void onTxnFailure(AsyncGroup ag, List<TxnFailureReport> reports) {
      this.logger.add(String.format("FooDataConn#onTxnFailure %d", this.id));
    }

    @Override
    public void close() {
      this.logger.add(String.format("FooDataConn#close %d", this.id));
    }
  }

  static class FooDataSrc implements DataSrc {
    final int id;
    final String text;
    boolean failToSetup;
    List<String> logger;

    FooDataSrc(int id, List<String> logger, boolean fail) {
      this.id = id;
      this.text = "hello";
      this.logger = logger;
      this.failToSetup = fail;
      logger.add(String.format("FooDataSrc#new %d", id));
    }

    @Override
    public void setup(AsyncGroup ag) throws Err {
      this.logger.add(String.format("FooDataSrc#setup %d", this.id));
    }

    @Override
    public void close() {
      this.logger.add(String.format("FooDataSrc#close %d", this.id));
    }

    @Override
    public DataConn createDataConn() throws Err {
      this.logger.add(String.format("FooDataSrc#createDataConn %d", this.id));
      return new FooDataConn(this.id, this.text, this.logger);
    }
  }

  static class BarDataConn implements DataConn {
    final int id;
    String text;
    boolean committed;
    List<String> logger;

    BarDataConn(int id, List<String> logger) {
      this.id = id;
      this.text = "";
      this.logger = logger;
      logger.add(String.format("BarDataConn#new %d", id));
    }

    public void setText(String v) {
      logger.add(String.format("BarDataConn#setText %d", this.id));
      this.text = v;
    }

    @Override
    public void commit(AsyncGroup ag) throws Err {
      this.committed = true;
      this.logger.add(String.format("BarDataConn#commit %d", this.id));
    }

    @Override
    public void preCommit(AsyncGroup ag) throws Err {
      this.logger.add(String.format("BarDataConn#preCommit %d", this.id));
    }

    @Override
    public void postCommit(AsyncGroup ag) throws Err {
      this.logger.add(String.format("BarDataConn#postCommit %d", this.id));
    }

    @Override
    public boolean isCommitted() {
      return this.committed;
    }

    @Override
    public void rollback(AsyncGroup ag) throws Err {
      this.logger.add(String.format("BarDataConn#rollback %d", this.id));
    }

    @Override
    public void onTxnFailure(AsyncGroup ag, List<TxnFailureReport> reports) {
      this.logger.add(String.format("BarDataConn#onTxnFailure %d", this.id));
    }

    @Override
    public void close() {
      this.logger.add(String.format("BarDataConn#close %d", this.id));
    }
  }

  static class BarDataSrc implements DataSrc {
    final int id;
    List<String> logger;
    boolean failToSetup;

    BarDataSrc(int id, List<String> logger, boolean fail) {
      this.id = id;
      this.logger = logger;
      this.failToSetup = fail;
      logger.add(String.format("BarDataSrc#new %d", id));
    }

    @Override
    public void setup(AsyncGroup ag) throws Err {
      this.logger.add(String.format("BarDataSrc#setup %d", this.id));
    }

    @Override
    public void close() {
      this.logger.add(String.format("BarDataSrc#close %d", this.id));
    }

    @Override
    public DataConn createDataConn() throws Err {
      this.logger.add(String.format("BarDataSrc#createDataConn %d", this.id));
      return new BarDataConn(this.id, this.logger);
    }
  }

  static interface SampleData {
    String getValue() throws Err;

    void setValue(String v) throws Err;
  }

  static Logic<SampleData> sampleLogic =
      data -> {
        String v = data.getValue();
        data.setValue(v);
        v = data.getValue();
        data.setValue(v);
      };

  static interface AllLogicData extends SampleData {}

  static interface FooDataAcc extends DataAcc, AllLogicData {
    @Override
    default String getValue() throws Err {
      var conn = getDataConn("foo", FooDataConn.class);
      return conn.getText();
    }
  }

  static interface BarDataAcc extends DataAcc, AllLogicData {
    @Override
    default void setValue(String v) throws Err {
      var conn = getDataConn("bar", BarDataConn.class);
      conn.setText(v);
      assertThat(conn.text).isEqualTo("hello");
    }
  }

  static class SampleDataHub extends DataHub implements FooDataAcc, BarDataAcc {}

  ///

  @Nested
  @SuppressWarnings("try")
  class RunTest {
    @BeforeEach
    void beforeEach() {
      DataHubInnerTest.resetGlobals();
    }

    @AfterEach
    void afterEach() {
      DataHubInnerTest.resetGlobals();
    }

    @Test
    void testMain() {
      var logger = new ArrayList<String>();

      uses("foo", new FooDataSrc(1, logger, false));

      try (var ac = setup()) {
        testApp(logger);
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(16);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("FooDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("FooDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("BarDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("BarDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("FooDataSrc#createDataConn 1");
      assertThat(iter.next()).isEqualTo("FooDataConn#new 1");
      assertThat(iter.next()).isEqualTo("FooDataConn#getText 1");
      assertThat(iter.next()).isEqualTo("BarDataSrc#createDataConn 2");
      assertThat(iter.next()).isEqualTo("BarDataConn#new 2");
      assertThat(iter.next()).isEqualTo("BarDataConn#setText 2");
      assertThat(iter.next()).isEqualTo("FooDataConn#getText 1");
      assertThat(iter.next()).isEqualTo("BarDataConn#setText 2");
      assertThat(iter.next()).isEqualTo("BarDataConn#close 2");
      assertThat(iter.next()).isEqualTo("FooDataConn#close 1");
      assertThat(iter.next()).isEqualTo("BarDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("FooDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    void testApp(List<String> logger) throws Err {
      try (var hub = new SampleDataHub()) {
        hub.uses("bar", new BarDataSrc(2, logger, false));
        hub.run(sampleLogic);
      } catch (Exception e) {
        fail(e);
      }
    }
  }

  @Nested
  @SuppressWarnings("try")
  class TxnTest {
    @BeforeEach
    void beforeEach() {
      DataHubInnerTest.resetGlobals();
    }

    @AfterEach
    void afterEach() {
      DataHubInnerTest.resetGlobals();
    }

    @Test
    void testMain() {
      var logger = new ArrayList<String>();

      uses("foo", new FooDataSrc(1, logger, false));

      try (var ac = setup()) {
        testApp(logger);
      } catch (Exception e) {
        fail(e);
      }

      assertThat(logger).hasSize(22);
      var iter = logger.iterator();
      assertThat(iter.next()).isEqualTo("FooDataSrc#new 1");
      assertThat(iter.next()).isEqualTo("FooDataSrc#setup 1");
      assertThat(iter.next()).isEqualTo("BarDataSrc#new 2");
      assertThat(iter.next()).isEqualTo("BarDataSrc#setup 2");
      assertThat(iter.next()).isEqualTo("FooDataSrc#createDataConn 1");
      assertThat(iter.next()).isEqualTo("FooDataConn#new 1");
      assertThat(iter.next()).isEqualTo("FooDataConn#getText 1");
      assertThat(iter.next()).isEqualTo("BarDataSrc#createDataConn 2");
      assertThat(iter.next()).isEqualTo("BarDataConn#new 2");
      assertThat(iter.next()).isEqualTo("BarDataConn#setText 2");
      assertThat(iter.next()).isEqualTo("FooDataConn#getText 1");
      assertThat(iter.next()).isEqualTo("BarDataConn#setText 2");
      assertThat(iter.next()).isEqualTo("FooDataConn#preCommit 1");
      assertThat(iter.next()).isEqualTo("BarDataConn#preCommit 2");
      assertThat(iter.next()).isEqualTo("FooDataConn#commit 1");
      assertThat(iter.next()).isEqualTo("BarDataConn#commit 2");
      assertThat(iter.next()).isEqualTo("FooDataConn#postCommit 1");
      assertThat(iter.next()).isEqualTo("BarDataConn#postCommit 2");
      assertThat(iter.next()).isEqualTo("BarDataConn#close 2");
      assertThat(iter.next()).isEqualTo("FooDataConn#close 1");
      assertThat(iter.next()).isEqualTo("BarDataSrc#close 2");
      assertThat(iter.next()).isEqualTo("FooDataSrc#close 1");
      assertThat(iter.hasNext()).isFalse();
    }

    void testApp(List<String> logger) throws Err {
      try (var hub = new SampleDataHub()) {
        hub.uses("bar", new BarDataSrc(2, logger, false));
        hub.txn(sampleLogic);
      } catch (Exception e) {
        fail(e);
      }
    }
  }
}
