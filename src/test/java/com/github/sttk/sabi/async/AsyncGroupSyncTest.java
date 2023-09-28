package com.github.sttk.sabi.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import com.github.sttk.sabi.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.Runner;

public class AsyncGroupSyncTest {

  @Test
  void should_be_ok() {
    var ag = new AsyncGroupSync();
    assertThat(ag.getErr()).isNull();

    final boolean[] exec = { false };
    final Runner fn = () -> {
      exec[0] = true;
    };

    ag.add(fn);
    assertThat(ag.getErr()).isNull();
    assertThat(exec[0]).isTrue();
  }

  record FailToDoSomething() {}

  @Test
  void should_be_error() {
    var ag = new AsyncGroupSync();
    assertThat(ag.getErr()).isNull();

    final boolean[] exec = { false };
    final Runner fn = () -> {
      exec[0] = true;
      throw new Err(new FailToDoSomething());
    };

    ag.add(fn);
    switch (ag.getErr().getReason()) {
    case FailToDoSomething reason:
      break;
    default:
      fail(ag.getErr().toString());
      break;
    }
    assertThat(exec[0]).isTrue();
  }

  @Test
  void should_be_error_by_runtimeexception() {
    var ag = new AsyncGroupSync();
    assertThat(ag.getErr()).isNull();

    final boolean[] exec = { false };
    final Runner fn = () -> {
      exec[0] = true;
      throw new RuntimeException();
    };

    ag.add(fn);
    switch (ag.getErr().getReason()) {
    case AsyncGroup.RunnerFailed reason:
      assertThat(ag.getErr().getCause().getClass())
        .isEqualTo(RuntimeException.class);
      break;
    default:
      fail(ag.getErr().toString());
      break;
    }
    assertThat(exec[0]).isTrue();
  }
}
