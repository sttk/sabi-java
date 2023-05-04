package sabi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;

public class ErrHandlerTest {

  static final List<String> syncLogger = new ArrayList<>();
  static final List<String> asyncLogger = new ArrayList<>();

  public record FailToDoSomething(String name) {}

  @Test
  public void should_notify_that_errs_are_created() {
    Err.addSyncErrHandler((err, occ) -> {
      syncLogger.add(String.format("1. %s %s",
        err.getReason().toString(), occ.toString()));
    });
    Err.addSyncErrHandler((err, occ) -> {
      syncLogger.add(String.format("2. %s %s",
        err.getReason().toString(), occ.toString()));
    });
    Err.addAsyncErrHandler((err, occ) -> {
      asyncLogger.add(String.format("3. %s %s",
        err.getReason().toString(), occ.toString()));
    });
    Err.addAsyncErrHandler((err, occ) -> {
      asyncLogger.add(String.format("4. %s %s",
        err.getReason().toString(), occ.toString()));
    });
    Err.fixErrCfgs();

    try {
      throw new Err(new FailToDoSomething("abc"));
    } catch (Err e) {
      assertThat(e.getReason()).isInstanceOf(FailToDoSomething.class);

      try {
        Thread.sleep(100);
      } catch (Exception e2) {}

      assertThat(syncLogger).hasSize(2);
      assertThat(syncLogger.get(0)).startsWith(
        "1. FailToDoSomething[name=abc] (ErrHandlerTest.java:38) ");
      assertThat(syncLogger.get(1)).startsWith(
        "2. FailToDoSomething[name=abc] (ErrHandlerTest.java:38) ");

      assertThat(asyncLogger).hasSize(2);
      assertThat(asyncLogger.get(0)).startsWith(
        "3. FailToDoSomething[name=abc] (ErrHandlerTest.java:38) ");
      assertThat(asyncLogger.get(1)).startsWith(
        "4. FailToDoSomething[name=abc] (ErrHandlerTest.java:38) ");
    }
  }
}
