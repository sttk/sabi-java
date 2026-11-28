package com.github.sttk.sabi;

import static com.github.sttk.sabi.TestDoubles.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Isolated
public class LogicRunnerIsolatedTest {
  private LogicRunnerIsolatedTest() {}

  @BeforeEach
  @AfterEach
  void reset() {
    Globals.DATA_SRC_MANAGER.close();
    Globals.DATA_SRCS_FIXED.set(false);
  }

  @SuppressWarnings("try")
  @Test
  void testDisusesAndEnableToUseGlobalDataSrcWhichHasSameName() {
    var logger = new ArrayList<String>();

    var ds1 = new SyncDataSrc(1, logger, Fail.None);
    Globals.uses("foo", ds1);

    var ds2 = new SyncDataSrc(2, logger, Fail.None);
    Globals.uses("bar", ds2);

    try (var ac = Globals.setup()) {
      try (var runner = new LogicRunner(new DataHub())) {
        var ds3 = new SyncDataSrc(3, logger, Fail.None);
        runner.uses("foo", ds3);

        var ds4 = new SyncDataSrc(4, logger, Fail.None);
        runner.uses("baz", ds4);

        runner.run(
            (DataAcc data) -> {
              var conn3 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              var conn4 = data.getDataConn("baz", SyncDataConn.class);
            });

        runner.disuses("foo");
        runner.disuses("bar");
        runner.disuses("baz");

        runner.run(
            (DataAcc data) -> {
              var conn3 = data.getDataConn("foo", SyncDataConn.class);
              var conn2 = data.getDataConn("bar", SyncDataConn.class);
              try {
                data.getDataConn("baz", SyncDataConn.class);
                fail();
              } catch (Err err) {
                switch (err.getReason()) {
                  case DataHubError.NoDataSrcToCreateDataConn r -> {
                    assertThat(r.name()).isEqualTo("baz");
                    assertThat(r.typeName()).isEqualTo(SyncDataConn.class.getName());
                  }
                  default -> fail(err);
                }
              }
            });
      }
    } catch (Exception e) {
      fail(e);
    }

    assertThat(logger)
        .containsExactly(
            "SyncDataSrc#new 1",
            "SyncDataSrc#new 2",
            "SyncDataSrc#setup 1",
            "SyncDataSrc#setup 2",
            "SyncDataSrc#new 3",
            "SyncDataSrc#new 4",
            "SyncDataSrc#setup 3",
            "SyncDataSrc#setup 4",
            "SyncDataSrc#createDataConn 3",
            "SyncDataConn#new 3",
            "SyncDataSrc#createDataConn 2",
            "SyncDataConn#new 2",
            "SyncDataSrc#createDataConn 4",
            "SyncDataConn#new 4",
            "SyncDataConn#close 4",
            "SyncDataConn#close 2",
            "SyncDataConn#close 3",
            "SyncDataSrc#close 3",
            "SyncDataSrc#close 4",
            "SyncDataSrc#createDataConn 1",
            "SyncDataConn#new 1",
            "SyncDataSrc#createDataConn 2",
            "SyncDataConn#new 2",
            "SyncDataConn#close 2",
            "SyncDataConn#close 1",
            "SyncDataSrc#close 2",
            "SyncDataSrc#close 1");
  }
}
