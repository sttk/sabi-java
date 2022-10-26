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
    Err.addSyncHandler((err, odt) -> {
      syncLogger.add("1. " + err.getReason().toString());
    });
    Err.addSyncHandler((err, odt) -> {
      syncLogger.add("2. " + err.getReason().toString());
    });
    Err.addAsyncHandler((err, odt) -> {
      asyncLogger.add("3. " + err.getReason().toString());
    });
    Err.addAsyncHandler((err, odt) -> {
      asyncLogger.add("4. " + err.getReason().toString());
    });
    Err.sealErrCfgs();

    try {
      throw new Err(new FailToDoSomething("abc"));
    } catch (Err e) {
      assertThat(e.getReason()).isInstanceOf(FailToDoSomething.class);

      try {
        Thread.sleep(100);
      } catch (Exception e2) {}

      assertThat(syncLogger).containsExactly(
        "1. FailToDoSomething[name=abc]",
        "2. FailToDoSomething[name=abc]");

      assertThat(asyncLogger).containsExactly(
        "3. FailToDoSomething[name=abc]",
        "4. FailToDoSomething[name=abc]");
    }
  }
}
