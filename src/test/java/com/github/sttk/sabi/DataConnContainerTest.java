package com.github.sttk.sabi;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.sttk.sabi.TestDoubles.*;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public class DataConnContainerTest {
  private DataConnContainerTest() {}

  @Test
  void testIsNoDataConn() {
    assertThat(DataConnContainer.isNoDataConn(null)).isTrue();

    var cont = new DataConnContainer("foo", null);
    assertThat(DataConnContainer.isNoDataConn(cont)).isTrue();

    var logger = new ArrayList<String>();
    var conn = new SyncDataConn(1, logger, Fail.None);
    cont = new DataConnContainer("foo", conn);
    assertThat(DataConnContainer.isNoDataConn(cont)).isFalse();
  }
}
