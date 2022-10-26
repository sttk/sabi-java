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
  void should_add_handlers_and_seal() {
    final var notifier = new ErrNotifier();
    assertThat(notifier.isSealed()).isFalse();
    assertThat(notifier.syncHandlers).isEmpty();
    assertThat(notifier.asyncHandlers).isEmpty();

    final ErrHandler h1 = (err, odt) -> {};
    notifier.addSyncHandler(h1);
    assertThat(notifier.isSealed()).isFalse();
    assertThat(notifier.syncHandlers).containsExactly(h1);
    assertThat(notifier.asyncHandlers).isEmpty();

    final ErrHandler h2 = (err, odt) -> {};
    notifier.addSyncHandler(h2);
    assertThat(notifier.isSealed()).isFalse();
    assertThat(notifier.syncHandlers).containsExactly(h1, h2);
    assertThat(notifier.asyncHandlers).isEmpty();

    final ErrHandler h3 = (err, odt) -> {};
    notifier.addAsyncHandler(h3);
    assertThat(notifier.isSealed()).isFalse();
    assertThat(notifier.syncHandlers).containsExactly(h1, h2);
    assertThat(notifier.asyncHandlers).containsExactly(h3);

    final ErrHandler h4 = (err, odt) -> {};
    notifier.addAsyncHandler(h4);
    assertThat(notifier.isSealed()).isFalse();
    assertThat(notifier.syncHandlers).containsExactly(h1, h2);
    assertThat(notifier.asyncHandlers).containsExactly(h3, h4);

    notifier.seal();

    final ErrHandler h5 = (err, odt) -> {};
    notifier.addSyncHandler(h5);
    assertThat(notifier.isSealed()).isTrue();
    assertThat(notifier.syncHandlers).containsExactly(h1, h2);
    assertThat(notifier.asyncHandlers).containsExactly(h3, h4);

    final ErrHandler h6 = (err, odt) -> {};
    notifier.addAsyncHandler(h6);
    assertThat(notifier.isSealed()).isTrue();
    assertThat(notifier.syncHandlers).containsExactly(h1, h2);
    assertThat(notifier.asyncHandlers).containsExactly(h3, h4);
  }

  // error reason
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
      notifier.addSyncHandler((err, odt) -> {
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

      notifier.seal();

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
      notifier.addAsyncHandler((err, odt) -> {
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

      notifier.seal();

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
      notifier.addAsyncHandler((err, odt) -> {
        logs.add("Async: " + err.getReason());
      });
      notifier.addSyncHandler((err, odt) -> {
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

      notifier.seal();

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
      notifier.addAsyncHandler((err, odt) -> {
        logs.add("Async: " + err.getReason());
      });
      notifier.addSyncHandler((err, odt) -> {
        logs.add("Sync(1): " + err.getReason());
      });
      notifier.addSyncHandler((err, odt) -> {
        throw new RuntimeException();
      });
      notifier.addSyncHandler((err, odt) -> {
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

      notifier.seal();

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
      notifier.addAsyncHandler((err, odt) -> {
        logs.add("Async(1): " + err.getReason());
      });
      notifier.addAsyncHandler((err, odt) -> {
        throw new RuntimeException();
      });
      notifier.addAsyncHandler((err, odt) -> {
        logs.add("Async(3): " + err.getReason());
      });
      notifier.addSyncHandler((err, odt) -> {
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

      notifier.seal();

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
