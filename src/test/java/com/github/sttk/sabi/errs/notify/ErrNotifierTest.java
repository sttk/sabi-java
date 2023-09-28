package com.github.sttk.sabi.errs.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;

import com.github.sttk.sabi.errs.Err;
import com.github.sttk.sabi.errs.ErrHandler;

import java.util.ArrayList;

public class ErrNotifierTest {

  @BeforeEach
  void reset() {
    try {
      var f0 = Err.class.getField("notifier");
      var n0 = f0.get(null);
      var f1 = ErrNotifier.class.getField("isFixed");
      f1.setBoolean(n0, false);
    } catch (Exception e) {}
  }

  @Test
  void should_add_handlers_and_fix() {
    final var notifier = new ErrNotifier();
    assertThat(notifier.isFixed()).isFalse();
    assertThat(notifier.syncErrHandlers).isEmpty();
    assertThat(notifier.asyncErrHandlers).isEmpty();

    final ErrHandler h1 = (err, occ) -> {};
    notifier.addSyncHandler(h1);
    assertThat(notifier.isFixed()).isFalse();
    assertThat(notifier.syncErrHandlers).containsExactly(h1);
    assertThat(notifier.asyncErrHandlers).isEmpty();

    final ErrHandler h2 = (err, occ) -> {};
    notifier.addSyncHandler(h2);
    assertThat(notifier.isFixed()).isFalse();
    assertThat(notifier.syncErrHandlers).containsExactly(h1, h2);
    assertThat(notifier.asyncErrHandlers).isEmpty();

    final ErrHandler h3 = (err, occ) -> {};
    notifier.addAsyncHandler(h3);
    assertThat(notifier.isFixed()).isFalse();
    assertThat(notifier.syncErrHandlers).containsExactly(h1, h2);
    assertThat(notifier.asyncErrHandlers).containsExactly(h3);

    final ErrHandler h4 = (err, occ) -> {};
    notifier.addAsyncHandler(h4);
    assertThat(notifier.isFixed()).isFalse();
    assertThat(notifier.syncErrHandlers).containsExactly(h1, h2);
    assertThat(notifier.asyncErrHandlers).containsExactly(h3, h4);

    notifier.fix();

    final ErrHandler h5 = (err, occ) -> {};
    notifier.addSyncHandler(h5);
    assertThat(notifier.isFixed()).isTrue();
    assertThat(notifier.syncErrHandlers).containsExactly(h1, h2);
    assertThat(notifier.asyncErrHandlers).containsExactly(h3, h4);

    final ErrHandler h6 = (err, occ) -> {};
    notifier.addAsyncHandler(h6);
    assertThat(notifier.isFixed()).isTrue();
    assertThat(notifier.syncErrHandlers).containsExactly(h1, h2);
    assertThat(notifier.asyncErrHandlers).containsExactly(h3, h4);
  }


  // error reasons
  record FailToDoSomething(String name) {}

  @Nested
  public class Notification {

    @Test
    void should_do_nothing_when_no_handlers() {
      final var notifier = new ErrNotifier();
      try {
        throw new Err(new FailToDoSomething("abc"));
      } catch (Err err) {
        try {
          notifier.notify(err);
        } catch (Exception e) {
          fail(e);
        }
      }
    }

    @Test
    void should_execute_sync_handlers() {
      final var logs = new ArrayList<String>();

      final var notifier = new ErrNotifier();
      notifier.addSyncHandler((err, occ) -> {
        var log = String.format("%s (%s:%d) %s", err.getReason().toString(),
          occ.getFile(), occ.getLine(), occ.getTime().toString());
        logs.add(log);
      });

      try {
        throw new Err(new FailToDoSomething("abc"));
      } catch (Err err) {
        try {
          notifier.notify(err);
        } catch (Exception e) {
          fail(e);
        }
      }

      assertThat(logs).isEmpty();

      notifier.fix();

      try {
        throw new Err(new FailToDoSomething("abc"));
      } catch (Err err) {
        try {
          notifier.notify(err);
        } catch (Exception e) {
          fail(e);
        }
      }

      assertThat(logs).hasSize(1);
      assertThat(logs.get(0)).startsWith(
        "FailToDoSomething[name=abc] (ErrNotifierTest.java:119) ");
    }

    @Test
    void should_execute_async_handlers() {
      final var logs = new ArrayList<String>();

      final var notifier = new ErrNotifier();
      notifier.addAsyncHandler((err, occ) -> {
        var log = String.format("%s (%s:%d) %s", err.getReason().toString(),
          occ.getFile(), occ.getLine(), occ.getTime().toString());
        logs.add(log);
      });

      try {
        throw new Err(new FailToDoSomething("abc"));
      } catch (Err err) {
        try {
          notifier.notify(err);
          Thread.sleep(100);
        } catch (Exception e) {
          fail(e);
        }
      }

      assertThat(logs).isEmpty();

      notifier.fix();

      try {
        throw new Err(new FailToDoSomething("abc"));
      } catch (Err err) {
        try {
          notifier.notify(err);
          Thread.sleep(100);
        } catch (Exception e) {
          fail(e);
        }
      }

      assertThat(logs).hasSize(1);
      assertThat(logs.get(0)).startsWith(
        "FailToDoSomething[name=abc] (ErrNotifierTest.java:160) ");
    }

    @Test
    void should_execute_sync_and_async_handlers() {
      final var logs = new ArrayList<String>();
      final var notifier = new ErrNotifier();
      notifier.addAsyncHandler((err, occ) -> {
        var log = String.format("Async: %s (%s:%d) %s",
          err.getReason().toString(), occ.getFile(), occ.getLine(),
          occ.getTime().toString());
        logs.add(log);
      });
      notifier.addSyncHandler((err, occ) -> {
        var log = String.format("Sync: %s (%s:%d) %s",
          err.getReason().toString(), occ.getFile(), occ.getLine(),
          occ.getTime().toString());
        logs.add(log);
      });

      try {
        throw new Err(new FailToDoSomething("abc"));
      } catch (Err err) {
        try {
          notifier.notify(err);
          Thread.sleep(100);
        } catch (Exception e) {
          fail(e);
        }
      }

      assertThat(logs).isEmpty();

      notifier.fix();

      try {
        throw new Err(new FailToDoSomething("abc"));
      } catch (Err err) {
        try {
          notifier.notify(err);
          Thread.sleep(100);
        } catch (Exception e) {
          fail(e);
        }
      }

      assertThat(logs).hasSize(2);
      assertThat(logs.get(0)).startsWith(
        "Sync: FailToDoSomething[name=abc] (ErrNotifierTest.java:208) ");
      assertThat(logs.get(1)).startsWith(
        "Async: FailToDoSomething[name=abc] (ErrNotifierTest.java:208) ");
    }

    @Test
    void should_stop_executing_sync_handlers_if_one_of_handlers_failed() {
      final var logs = new ArrayList<String>();
      final var notifier = new ErrNotifier();
      notifier.addAsyncHandler((err, occ) -> {
        var log = String.format("Async: %s (%s:%d) %s",
          err.getReason().toString(), occ.getFile(), occ.getLine(),
          occ.getTime().toString());
        logs.add(log);
      });
      notifier.addSyncHandler((err, occ) -> {
        var log = String.format("Sync(1): %s (%s:%d) %s",
          err.getReason().toString(), occ.getFile(), occ.getLine(),
          occ.getTime().toString());
        logs.add(log);
      });
      notifier.addSyncHandler((err, occ) -> {
        throw new RuntimeException();
      });
      notifier.addSyncHandler((err, occ) -> {
        var log = String.format("Sync(3): %s (%s:%d) %s",
          err.getReason().toString(), occ.getFile(), occ.getLine(),
          occ.getTime().toString());
        logs.add(log);
      });

      try {
        throw new Err(new FailToDoSomething("abc"));
      } catch (Err err) {
        try {
          notifier.notify(err);
          Thread.sleep(100);
        } catch (Exception e) {
          fail(e);
        }
      }

      assertThat(logs).isEmpty();

      notifier.fix();

      try {
        throw new Err(new FailToDoSomething("abc"));
      } catch (Err err) {
        try {
          notifier.notify(err);
          Thread.sleep(100);
          fail();
        } catch (RuntimeException e) {
          assertThat(e).isNotNull();
        } catch (Exception e) {
          fail(e);
        }
      }

      assertThat(logs).hasSize(1);
      assertThat(logs.get(0)).startsWith(
        "Sync(1): FailToDoSomething[name=abc] (ErrNotifierTest.java:267) ");
    }

    @Test
    void should_execute_all_async_handlers_even_if_one_of_handlers_failed() {
      final var logs = new ArrayList<String>();
      final var notifier = new ErrNotifier();
      notifier.addAsyncHandler((err, occ) -> {
        try { Thread.sleep(10); } catch (Exception e) {}
        var log = String.format("Async(1): %s (%s:%d) %s",
          err.getReason().toString(), occ.getFile(), occ.getLine(),
          occ.getTime().toString());
        logs.add(log);
      });
      notifier.addAsyncHandler((err, occ) -> {
        throw new RuntimeException(
          "**This exception is not a error but for test purpose.**");
      });
      notifier.addAsyncHandler((err, occ) -> {
        try { Thread.sleep(100); } catch (Exception e) {}
        var log = String.format("Async(3): %s (%s:%d) %s",
          err.getReason().toString(), occ.getFile(), occ.getLine(),
          occ.getTime().toString());
        logs.add(log);
      });
      notifier.addSyncHandler((err, occ) -> {
        var log = String.format("Sync: %s (%s:%d) %s",
          err.getReason().toString(), occ.getFile(), occ.getLine(),
          occ.getTime().toString());
        logs.add(log);
      });

      try {
        throw new Err(new FailToDoSomething("abc"));
      } catch (Err err) {
        try {
          notifier.notify(err);
          Thread.sleep(100);
        } catch (Exception e) {
          fail(e);
        }
      }

      assertThat(logs).isEmpty();

      notifier.fix();

      try {
        throw new Err(new FailToDoSomething("abc"));
      } catch (Err err) {
        try {
          notifier.notify(err);
          Thread.sleep(100);
        } catch (Exception e) {
          fail(e);
        }
      }

      try { Thread.sleep(200); } catch (Exception e) {}

      assertThat(logs).hasSize(3);
      assertThat(logs.get(0)).startsWith(
        "Sync: FailToDoSomething[name=abc] (ErrNotifierTest.java:330) ");
      assertThat(logs.get(1)).startsWith(
        "Async(1): FailToDoSomething[name=abc] (ErrNotifierTest.java:330) ");
      assertThat(logs.get(2)).startsWith(
        "Async(3): FailToDoSomething[name=abc] (ErrNotifierTest.java:330) ");
    }
  }
}
