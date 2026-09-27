package com.github.sttk.sabi.internal;

import static com.github.sttk.sabi.Sabi.FailToSetupGlobalDataSrcs;
import static com.github.sttk.sabi.Sabi.setup;
import static com.github.sttk.sabi.Sabi.uses;
import static com.github.sttk.sabi.internal.TestCommonsTest.Fail;
import static com.github.sttk.sabi.internal.TestCommonsTest.SyncDataSrc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public class GlobalSetupTest {
  private GlobalSetupTest() {}

  static void resetGlobals() {
    GlobalSetup.GLOBAL_DATA_SRC_MANAGER.close();
    GlobalSetup.GLOBAL_DATA_SRCS_FIXED.set(false);
  }

  @Test
  void testUsesAndSetupAndOk() {
    var logger = new ArrayList<String>();
    try {
      resetGlobals();

      assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

      uses("foo", new SyncDataSrc(1, logger, Fail.None));

      assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(1);
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(0);

      try (var ac = setup()) {
        assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(0);
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(1);
      }
    } catch (Exception e) {
      fail(e);
    } finally {
      resetGlobals();
    }

    assertThat(logger).hasSize(3);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testUsesAndSetupButFail() {
    var logger = new ArrayList<String>();
    try {
      resetGlobals();

      assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

      uses("foo", new SyncDataSrc(1, logger, Fail.Setup));

      assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(1);
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(0);

      try (var ac = setup()) {
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToSetupGlobalDataSrcs r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index).isEqualTo(0);
            assertThat(r.errors().get(0).name).isEqualTo("foo");
            assertThat(r.errors().get(0).err.getReason()).isEqualTo("XXX");
          }
          default -> fail(err);
        }
        assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(0);
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(0);
      }
    } catch (Exception e) {
      fail(e);
    } finally {
      resetGlobals();
    }

    assertThat(logger).hasSize(2);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1 failed");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testUsesAndSetupButAlreadyFixedBefore() {
    var logger = new ArrayList<String>();
    try {
      resetGlobals();

      assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

      try (var ac = setup()) {
        assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

        uses("foo", new SyncDataSrc(1, logger, Fail.Setup));

        assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();
      }
    } catch (Exception e) {
      fail(e);
    } finally {
      resetGlobals();
    }

    assertThat(logger).hasSize(1);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testUsesAndSetupWithOrderAndOk() {
    var logger = new ArrayList<String>();
    try {
      resetGlobals();

      assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

      uses("foo", new SyncDataSrc(1, logger, Fail.None));
      uses("bar", new SyncDataSrc(2, logger, Fail.None));

      assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(2);
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(0);

      try (var ac = setup("bar", "foo")) {
        assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(0);
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(2);
      }
    } catch (Exception e) {
      fail(e);
    } finally {
      resetGlobals();
    }

    assertThat(logger).hasSize(6);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#close 2");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testUsesAndSetupWithOrderButFail() {
    var logger = new ArrayList<String>();
    try {
      resetGlobals();

      assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

      uses("foo", new SyncDataSrc(1, logger, Fail.Setup));
      uses("bar", new SyncDataSrc(2, logger, Fail.Setup));

      assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(2);
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(0);

      try (var ac = setup("bar", "foo")) {
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case FailToSetupGlobalDataSrcs r -> {
            assertThat(r.errors()).hasSize(1);
            assertThat(r.errors().get(0).index).isEqualTo(0);
            assertThat(r.errors().get(0).name).isEqualTo("bar");
            assertThat(r.errors().get(0).err.getReason()).isEqualTo("XXX");
          }
          default -> fail(err);
        }
        assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).hasSize(0);
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).hasSize(0);
      }
    } catch (Exception e) {
      fail(e);
    } finally {
      resetGlobals();
    }

    assertThat(logger).hasSize(3);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 2");
    assertThat(iter.next()).isEqualTo("SyncDataSrc#setup 2 failed");
    assertThat(iter.hasNext()).isFalse();
  }

  @Test
  void testUsesAndSetupWithOrderButAlreadyFixedBefore() {
    var logger = new ArrayList<String>();
    try {
      resetGlobals();

      assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
      assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

      try (var ac = setup("bar", "foo")) {
        assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();

        uses("foo", new SyncDataSrc(1, logger, Fail.Setup));

        assertThat(GlobalSetup.GLOBAL_DATA_SRCS_FIXED.get()).isTrue();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.local).isFalse();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listUnready).isEmpty();
        assertThat(GlobalSetup.GLOBAL_DATA_SRC_MANAGER.listReady).isEmpty();
      }
    } catch (Exception e) {
      fail(e);
    } finally {
      resetGlobals();
    }

    assertThat(logger).hasSize(1);
    var iter = logger.iterator();
    assertThat(iter.next()).isEqualTo("SyncDataSrc#new 1");
    assertThat(iter.hasNext()).isFalse();
  }
}
