package com.github.sttk.sabi.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.DataConn;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DataConnListTest {
  private DataConnListTest() {}

  static class SampleDataConn implements DataConn {
    private int id;
    private List<String> logger;

    public SampleDataConn(int id, List<String> logger) {
      this.id = id;
      this.logger = logger;
    }

    public void commit(AsyncGroup ag) throws Exc {}

    public void preCommit(AsyncGroup ag) throws Exc {}

    public void postCommit(AsyncGroup ag) {}

    public boolean shouldForceBack() {
      return false;
    }

    public void rollback(AsyncGroup ag) {}

    public void forceBack(AsyncGroup ag) {}

    public void close() {
      this.logger.add(String.format("SampleDataConn %d closed", this.id));
    }
  }

  @Test
  void test_new() {
    var dcList = new DataConnList();

    assertThat(dcList.head).isNull();
    assertThat(dcList.last).isNull();
  }

  @Test
  void test_appendContainer() {
    var dcList = new DataConnList();
    var logger = new ArrayList<String>();

    var dc1 = new SampleDataConn(1, logger);
    var cont1 = new DataConnContainer("foo", dc1);
    dcList.appendContainer(cont1);

    assertThat(dcList.head).isEqualTo(cont1);
    assertThat(dcList.last).isEqualTo(cont1);

    var dc2 = new SampleDataConn(2, logger);
    var cont2 = new DataConnContainer("bar", dc2);
    dcList.appendContainer(cont2);

    assertThat(dcList.head).isEqualTo(cont1);
    assertThat(dcList.last).isEqualTo(cont2);
    assertThat(cont1.prev).isNull();
    assertThat(cont1.next).isEqualTo(cont2);
    assertThat(cont2.prev).isEqualTo(cont1);
    assertThat(cont2.next).isNull();

    var dc3 = new SampleDataConn(3, logger);
    var cont3 = new DataConnContainer("baz", dc3);
    dcList.appendContainer(cont3);

    assertThat(dcList.head).isEqualTo(cont1);
    assertThat(dcList.last).isEqualTo(cont3);
    assertThat(cont1.prev).isNull();
    assertThat(cont1.next).isEqualTo(cont2);
    assertThat(cont2.prev).isEqualTo(cont1);
    assertThat(cont2.next).isEqualTo(cont3);
    assertThat(cont3.prev).isEqualTo(cont2);
    assertThat(cont3.next).isNull();

    dcList.closeDataConns();

    assertThat(dcList.head).isNull();
    assertThat(dcList.last).isNull();

    assertThat(logger)
        .containsExactly(
            "SampleDataConn 3 closed", "SampleDataConn 2 closed", "SampleDataConn 1 closed");
  }
}
