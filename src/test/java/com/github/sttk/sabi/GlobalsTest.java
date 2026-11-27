package com.github.sttk.sabi;

import static com.github.sttk.sabi.TestDoubles.*;
import static com.github.sttk.sabi.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Isolated
public class GlobalsTest {
  private GlobalsTest() {}

  @BeforeEach
  @AfterEach
  void reset() {
    Globals.DATA_SRC_MANAGER.close();
    Globals.DATA_SRCS_FIXED.set(false);
  }

  @SuppressWarnings("try")
  @Test
  void testSetupWithNoDs() {
    assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    try (var ac = Globals.setup()) {
      assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
      assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
      assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
      assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
      assertThat(Globals.DATA_SRCS_FIXED.get()).isTrue();
    } catch (Err err) {
      fail(err);
    } catch (Exception e) {
      fail(e);
    }

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isTrue();
  }

  @SuppressWarnings("try")
  @Test
  void testUsesAndSetup() {
    var logger = new ArrayList<String>();

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    Globals.uses("ds1", new SyncDataSrc(1, logger, Fail.None));

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).hasSize(1);
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    Globals.uses("ds2", new SyncDataSrc(2, logger, Fail.None));

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).hasSize(2);
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    try (var ac = Globals.setup()) {
      assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
      assertThat(Globals.DATA_SRC_MANAGER.listReady).hasSize(2);
      assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
      assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
      assertThat(Globals.DATA_SRCS_FIXED.get()).isTrue();
    } catch (Err err) {
      fail(err);
    } catch (Exception e) {
      fail(e);
    }

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isTrue();

    assertThat(logger)
        .containsExactly(
            "SyncDataSrc#new 1",
            "SyncDataSrc#new 2",
            "SyncDataSrc#setup 1",
            "SyncDataSrc#setup 2",
            "SyncDataSrc#close 2",
            "SyncDataSrc#close 1");
  }

  @SuppressWarnings("try")
  @Test
  void testUsesAndSetupWithOrder() {
    var logger = new ArrayList<String>();

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    Globals.uses("ds1", new SyncDataSrc(1, logger, Fail.None));

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).hasSize(1);
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    Globals.uses("ds2", new SyncDataSrc(2, logger, Fail.None));

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).hasSize(2);
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    try (var ac = Globals.setupWithOrder("ds2", "ds1")) {
      assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
      assertThat(Globals.DATA_SRC_MANAGER.listReady).hasSize(2);
      assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
      assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
      assertThat(Globals.DATA_SRCS_FIXED.get()).isTrue();
    } catch (Err err) {
      fail(err);
    } catch (Exception e) {
      fail(e);
    }

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isTrue();

    assertThat(logger)
        .containsExactly(
            "SyncDataSrc#new 1",
            "SyncDataSrc#new 2",
            "SyncDataSrc#setup 2",
            "SyncDataSrc#setup 1",
            "SyncDataSrc#close 1",
            "SyncDataSrc#close 2");
  }

  @SuppressWarnings("try")
  @Test
  void testUsesButAlreadyFixed() {
    var logger = new ArrayList<String>();

    Globals.uses("ds1", new SyncDataSrc(1, logger, Fail.None));

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).hasSize(1);
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    try (var ac = Globals.setup()) {
      assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
      assertThat(Globals.DATA_SRC_MANAGER.listReady).hasSize(1);
      assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
      assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
      assertThat(Globals.DATA_SRCS_FIXED.get()).isTrue();

      Globals.uses("ds2", new SyncDataSrc(2, logger, Fail.None));

      assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
      assertThat(Globals.DATA_SRC_MANAGER.listReady).hasSize(1);
      assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
      assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
      assertThat(Globals.DATA_SRCS_FIXED.get()).isTrue();
    } catch (Exception e) {
    }

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isTrue();
  }

  @SuppressWarnings("try")
  @Test
  void testSetupButFailed() {
    var logger = new ArrayList<String>();

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    Globals.uses("ds1", new SyncDataSrc(1, logger, Fail.None));

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).hasSize(1);
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    Globals.uses("ds2", new SyncDataSrc(2, logger, Fail.Setup));

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).hasSize(2);
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    Globals.uses("ds3", new SyncDataSrc(3, logger, Fail.Setup));

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).hasSize(3);
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    try (var ac = Globals.setup()) {
      fail();
    } catch (Err err) {
      assertThat(err.getReason()).isInstanceOf(DataHubError.FailToSetupGlobalDataSrcs.class);
      assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
      assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
      assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
      assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
      assertThat(Globals.DATA_SRCS_FIXED.get()).isTrue();
    } catch (Exception e) {
      fail(e);
    }

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isTrue();

    try (var ac = Globals.setup()) {
    } catch (Exception e) {
      fail(e);
    }

    assertThat(logger)
        .containsExactly(
            "SyncDataSrc#new 1",
            "SyncDataSrc#new 2",
            "SyncDataSrc#new 3",
            "SyncDataSrc#setup 1",
            "SyncDataSrc#setup 2 failed",
            "SyncDataSrc#close 2",
            "SyncDataSrc#close 1");
  }

  @SuppressWarnings("try")
  @Test
  void testSetupWithOrderButFailed() {
    var logger = new ArrayList<String>();

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    Globals.uses("ds1", new SyncDataSrc(1, logger, Fail.None));

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).hasSize(1);
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    Globals.uses("ds2", new SyncDataSrc(2, logger, Fail.Setup));

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).hasSize(2);
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    Globals.uses("ds3", new SyncDataSrc(3, logger, Fail.None));

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).hasSize(3);
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isFalse();

    try (var ac = Globals.setupWithOrder("ds3", "ds2", "ds1")) {
      fail();
    } catch (Err err) {
      assertThat(err.getReason()).isInstanceOf(DataHubError.FailToSetupGlobalDataSrcs.class);
      assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
      assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
      assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
      assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
      assertThat(Globals.DATA_SRCS_FIXED.get()).isTrue();
    } catch (Exception e) {
      fail(e);
    }

    assertThat(Globals.DATA_SRC_MANAGER.listUnready).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.listReady).isEmpty();
    assertThat(Globals.DATA_SRC_MANAGER.local).isFalse();
    assertThat(Globals.DATA_SRC_MANAGER.isAbnormal).isFalse();
    assertThat(Globals.DATA_SRCS_FIXED.get()).isTrue();

    try (var ac = Globals.setupWithOrder("ds3", "ds2", "ds1")) {
    } catch (Exception e) {
      fail(e);
    }

    assertThat(logger)
        .containsExactly(
            "SyncDataSrc#new 1",
            "SyncDataSrc#new 2",
            "SyncDataSrc#new 3",
            "SyncDataSrc#setup 3",
            "SyncDataSrc#setup 2 failed",
            "SyncDataSrc#close 2",
            "SyncDataSrc#close 3");
  }

  @SuppressWarnings("try")
  @Test
  void testUsingGlobalAndLocalDs() {
    var logger = new ArrayList<String>();

    Globals.uses("ds1", new SyncDataSrc(1, logger, Fail.None));
    Globals.uses("ds2", new SyncDataSrc(2, logger, Fail.None));
    try (var ac = Globals.setup()) {
      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("ds3", new SyncDataSrc(3, logger, Fail.None));
        runner.uses("ds4", new SyncDataSrc(4, logger, Fail.None));
        runner.run(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("ds1", SyncDataConn.class);
              var conn3 = data.getDataConn("ds3", SyncDataConn.class);
            });
      } catch (Exception e) {
        fail(e);
      }

      try (var runner = new LogicRunner(new DataHub())) {
        runner.uses("ds5", new SyncDataSrc(5, logger, Fail.None));
        runner.uses("ds6", new SyncDataSrc(6, logger, Fail.None));
        runner.run(
            (DataAcc data) -> {
              var conn1 = data.getDataConn("ds1", SyncDataConn.class);
              var conn2 = data.getDataConn("ds2", SyncDataConn.class);
              var conn5 = data.getDataConn("ds5", SyncDataConn.class);
              try {
                var conn3 = data.getDataConn("ds3", SyncDataConn.class);
              } catch (Err e) {
                System.out.println(e);
              }
              try {
                var conn4 = data.getDataConn("ds4", SyncDataConn.class);
              } catch (Err e) {
                System.out.println(e);
              }
            });
      } catch (Exception e) {
        fail(e);
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
            "SyncDataSrc#createDataConn 1",
            "SyncDataConn#new 1",
            "SyncDataSrc#createDataConn 3",
            "SyncDataConn#new 3",
            "SyncDataConn#close 3",
            "SyncDataConn#close 1",
            "SyncDataSrc#close 4",
            "SyncDataSrc#close 3",
            "SyncDataSrc#new 5",
            "SyncDataSrc#new 6",
            "SyncDataSrc#setup 5",
            "SyncDataSrc#setup 6",
            "SyncDataSrc#createDataConn 1",
            "SyncDataConn#new 1",
            "SyncDataSrc#createDataConn 2",
            "SyncDataConn#new 2",
            "SyncDataSrc#createDataConn 5",
            "SyncDataConn#new 5",
            "SyncDataConn#close 5",
            "SyncDataConn#close 2",
            "SyncDataConn#close 1",
            "SyncDataSrc#close 6",
            "SyncDataSrc#close 5",
            "SyncDataSrc#close 2",
            "SyncDataSrc#close 1");
  }
}
