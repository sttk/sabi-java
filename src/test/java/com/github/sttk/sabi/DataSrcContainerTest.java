package com.github.sttk.sabi;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.sttk.sabi.TestDoubles.*;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public class DataSrcContainerTest {
  private DataSrcContainerTest() {}

  @Test
  void testIsNoDataSrc() {
    assertThat(DataSrcContainer.isNoDataSrc(null)).isTrue();

    var cont = new DataSrcContainer(true, "foo", null);
    assertThat(DataSrcContainer.isNoDataSrc(cont)).isTrue();

    var logger = new ArrayList<String>();
    var ds = new SyncDataSrc(1, logger, Fail.None);
    cont = new DataSrcContainer(true, "foo", ds);
    assertThat(DataSrcContainer.isNoDataSrc(cont)).isFalse();
  }
}
