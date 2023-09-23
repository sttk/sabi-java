package errs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.ArrayList;

import com.github.sttk.sabi.errs.Err;
import com.github.sttk.sabi.errs.notify.ErrNotifier;

public class ErrHandlerTest {

  static final List<String> syncLogger = new ArrayList<>();
  static final List<String> asyncLogger = new ArrayList<>();

  public record FailToDoSomething(String name) {}

  @BeforeEach
  void reset() {
    try {
      var f0 = Err.class.getDeclaredField("notifier");
      f0.setAccessible(true);
      var n0 = f0.get(null);
      var f1 = ErrNotifier.class.getDeclaredField("isFixed");
      f1.setAccessible(true);
      f1.setBoolean(n0, false);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void should_notify_that_errs_are_created() {
    Err.addSyncHandler((err, occ) -> {
      syncLogger.add(String.format("1. %s (%s:%d)",
        err.getReason().toString(), occ.getFile(), occ.getLine()));
    });
    Err.addSyncHandler((err, occ) -> {
      syncLogger.add(String.format("2. %s (%s:%d)",
        err.getReason().toString(), occ.getFile(), occ.getLine()));
    });
    Err.addAsyncHandler((err, occ) -> {
      try { Thread.sleep(10); } catch (Exception e) {}
      asyncLogger.add(String.format("3. %s (%s:%d)",
        err.getReason().toString(), occ.getFile(), occ.getLine()));
    });
    Err.addAsyncHandler((err, occ) -> {
      try { Thread.sleep(20); } catch (Exception e) {}
      asyncLogger.add(String.format("4. %s (%s:%d)",
        err.getReason().toString(), occ.getFile(), occ.getLine()));
    });
    Err.fixCfg();

    try {
      throw new Err(new FailToDoSomething("abc"));
    } catch (Err e) {
      assertThat(e.getReason()).isInstanceOf(FailToDoSomething.class);

      try {
        Thread.sleep(100);
      } catch (Exception e2) {}

      assertThat(syncLogger).hasSize(2);
      assertThat(syncLogger.get(0)).startsWith(
        "1. FailToDoSomething[name=abc] (ErrHandlerTest.java:58)");
      assertThat(syncLogger.get(1)).startsWith(
        "2. FailToDoSomething[name=abc] (ErrHandlerTest.java:58)");

      assertThat(asyncLogger).hasSize(2);
      assertThat(asyncLogger.get(0)).startsWith(
        "3. FailToDoSomething[name=abc] (ErrHandlerTest.java:58)");
      assertThat(asyncLogger.get(1)).startsWith(
        "4. FailToDoSomething[name=abc] (ErrHandlerTest.java:58)");
    }
  }
}
