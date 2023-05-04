package sabi.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import sabi.Err;
import sabi.ErrHandler;

import java.util.ArrayList;

public class ErrNotifierTest {

  @Test
  void should_add_err_handlers_and_fix() {
    final var notifier = new ErrNotifier();
    assertThat(notifier.isFixed()).isFalse();
    assertThat(notifier.syncErrHandlers).isEmpty();
    assertThat(notifier.asyncErrHandlers).isEmpty();

    final ErrHandler h1 = (err, occ) -> {};
    notifier.addSyncErrHandler(h1);
    assertThat(notifier.isFixed()).isFalse();
    assertThat(notifier.syncErrHandlers).containsExactly(h1);
    assertThat(notifier.asyncErrHandlers).isEmpty();

    final ErrHandler h2 = (err, occ) -> {};
    notifier.addSyncErrHandler(h2);
    assertThat(notifier.isFixed()).isFalse();
    assertThat(notifier.syncErrHandlers).containsExactly(h1, h2);
    assertThat(notifier.asyncErrHandlers).isEmpty();

    final ErrHandler h3 = (err, occ) -> {};
    notifier.addAsyncErrHandler(h3);
    assertThat(notifier.isFixed()).isFalse();
    assertThat(notifier.syncErrHandlers).containsExactly(h1, h2);
    assertThat(notifier.asyncErrHandlers).containsExactly(h3);

    final ErrHandler h4 = (err, occ) -> {};
    notifier.addAsyncErrHandler(h4);
    assertThat(notifier.isFixed()).isFalse();
    assertThat(notifier.syncErrHandlers).containsExactly(h1, h2);
    assertThat(notifier.asyncErrHandlers).containsExactly(h3, h4);

    notifier.fix();

    final ErrHandler h5 = (err, occ) -> {};
    notifier.addSyncErrHandler(h5);
    assertThat(notifier.isFixed()).isTrue();
    assertThat(notifier.syncErrHandlers).containsExactly(h1, h2);
    assertThat(notifier.asyncErrHandlers).containsExactly(h3, h4);

    final ErrHandler h6 = (err, occ) -> {};
    notifier.addAsyncErrHandler(h6);
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
      notifier.addSyncErrHandler((err, occ) -> {
        logs.add(err.getReason().toString());
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

      assertThat(logs).containsExactly("FailToDoSomething[name=abc]");
    }

    @Test
    void should_execute_async_handlers() {
      final var logs = new ArrayList<String>();

      final var notifier = new ErrNotifier();
      notifier.addAsyncErrHandler((err, occ) -> {
        logs.add(err.getReason().toString());
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

      assertThat(logs).containsExactly("FailToDoSomething[name=abc]");
    }

    @Test
    void should_execute_sync_and_async_handlers() {
      final var logs = new ArrayList<String>();
      final var notifier = new ErrNotifier();
      notifier.addAsyncErrHandler((err, occ) -> {
        logs.add("Async: " + err.getReason());
      });
      notifier.addSyncErrHandler((err, occ) -> {
        logs.add("Sync: " + err.getReason());
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

      assertThat(logs).containsOnly(
        "Async: FailToDoSomething[name=abc]",
        "Sync: FailToDoSomething[name=abc]"
      );
    }

    @Test
    void should_stop_executing_sync_handlers_if_one_of_handlers_failed() {
      final var logs = new ArrayList<String>();
      final var notifier = new ErrNotifier();
      notifier.addAsyncErrHandler((err, occ) -> {
        logs.add("Async: " + err.getReason());
      });
      notifier.addSyncErrHandler((err, occ) -> {
        logs.add("Sync(1): " + err.getReason());
      });
      notifier.addSyncErrHandler((err, occ) -> {
        throw new RuntimeException();
      });
      notifier.addSyncErrHandler((err, odt) -> {
        logs.add("Sync(3): " + err.getReason());
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

      assertThat(logs).containsOnly(
        "Sync(1): FailToDoSomething[name=abc]"
      );
    }

    @Test
    void should_execute_all_async_handlers_even_if_one_of_handlers_failed() {
      final var logs = new ArrayList<String>();
      final var notifier = new ErrNotifier();
      notifier.addAsyncErrHandler((err, occ) -> {
        logs.add("Async(1): " + err.getReason());
      });
      notifier.addAsyncErrHandler((err, occ) -> {
        throw new RuntimeException();
      });
      notifier.addAsyncErrHandler((err, occ) -> {
        logs.add("Async(3): " + err.getReason());
      });
      notifier.addSyncErrHandler((err, occ) -> {
        logs.add("Sync: " + err.getReason());
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

      assertThat(logs).containsOnly(
        "Sync: FailToDoSomething[name=abc]",
        "Async(1): FailToDoSomething[name=abc]",
        "Async(3): FailToDoSomething[name=abc]"
      );
    }
  }
}
