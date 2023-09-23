package com.github.sttk.sabi.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import com.github.sttk.sabi.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.Runner;

public class AsyncGroupAsyncTest {

  @Test
  void should_be_ok() {
    var ag = new AsyncGroupAsync<String>();
    assertThat(ag.hasErr()).isFalse();

    final boolean[] exec = {false};
    final Runner fn = () -> {
      try {
        Thread.sleep(50);
      } catch (Exception e) {}
      exec[0] = true;
    };

    ag.name = "foo";
    ag.add(fn);
    assertThat(ag.hasErr()).isFalse();
    assertThat(exec[0]).isFalse();

    ag.join();
    assertThat(ag.hasErr()).isFalse();
    assertThat(exec[0]).isTrue();

    assertThat(ag.makeErrs()).hasSize(0);
    assertThat(exec[0]).isTrue();
  }

  record FailToDoSomething() {}

  @Test
  void should_be_error() {
    var ag = new AsyncGroupAsync<String>();
    assertThat(ag.hasErr()).isFalse();

    final boolean[] exec = {false};
    final Runner fn = () -> {
      try {
        Thread.sleep(50);
      } catch (Exception e) {}
      exec[0] = true;
      throw new Err(new FailToDoSomething());
    };

    ag.name = "foo";
    ag.add(fn);
    assertThat(ag.hasErr()).isFalse();
    assertThat(exec[0]).isFalse();

    ag.join();
    assertThat(ag.hasErr()).isTrue();
    assertThat(exec[0]).isTrue();

    var m = ag.makeErrs();
    assertThat(m).hasSize(1);
    switch (m.get("foo").getReason()) {
      case FailToDoSomething reason:
        break;
      default:
        fail(m.get("foo").toString());
        break;
    }
    assertThat(exec[0]).isTrue();
  }

  record Err0() {}
  record Err1() {}

  @Test
  void should_get_multipleErrors() {
    var ag = new AsyncGroupAsync<String>();
    assertThat(ag.hasErr()).isFalse();

    final boolean[] exec = {false, false, false};
    final Runner fn0 = () -> {
      try {
        Thread.sleep(200);
      } catch (Exception e) {}
      exec[0] = true;
      throw new Err(new Err0());
    };
    final Runner fn1 = () -> {
      try {
        Thread.sleep(400);
      } catch (Exception e) {}
      exec[1] = true;
      throw new Err(new Err1());
    };
    final Runner fn2 = () -> {
      try {
        Thread.sleep(800);
      } catch (Exception e) {}
      exec[2] = true;
      throw new RuntimeException();
    };

    ag.name = "foo0";
    ag.add(fn0);
    ag.name = "foo1";
    ag.add(fn1);
    ag.name = "foo2";
    ag.add(fn2);
    assertThat(ag.hasErr()).isFalse();
    assertThat(exec[0]).isFalse();
    assertThat(exec[1]).isFalse();
    assertThat(exec[2]).isFalse();

    ag.join();
    assertThat(ag.hasErr()).isTrue();
    assertThat(exec[0]).isTrue();
    assertThat(exec[1]).isTrue();
    assertThat(exec[2]).isTrue();

    var m = ag.makeErrs();
    assertThat(m).hasSize(3);
    assertThat(m.get("foo0").getReasonName()).isEqualTo("Err0");
    assertThat(m.get("foo1").getReasonName()).isEqualTo("Err1");
    assertThat(m.get("foo2").getReasonName()).isEqualTo("RunnerFailed");
    assertThat(exec[0]).isTrue();
    assertThat(exec[1]).isTrue();
    assertThat(exec[2]).isTrue();
  }
}
