package com.github.sttk.sabi.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.Runner;
import org.junit.jupiter.api.Test;

public class AsyncGroupImplTest {
  private AsyncGroupImplTest() {}

  @Test
  void zero() {
    var ag = new AsyncGroupImpl();

    var errors = ag.join();
    assertThat(errors).isEmpty();
  }

  @Test
  void ok() {
    var ag = new AsyncGroupImpl();

    boolean[] executed = {false};
    Runner fn =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
          }
          executed[0] = true;
        };

    ag._index = 123;
    ag._name = "foo";
    ag.add(fn);
    assertThat(executed[0]).isFalse();

    var errors = ag.join();
    assertThat(executed[0]).isTrue();
    assertThat(errors).isEmpty();
  }

  @Test
  void error() {
    var ag = new AsyncGroupImpl();

    record FailToDoSomething() {}

    boolean[] executed = {false};
    Runner fn =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
          }
          executed[0] = true;
          throw new Err(new FailToDoSomething());
        };

    ag._index = 123;
    ag._name = "foo";
    ag.add(fn);
    assertThat(executed[0]).isFalse();

    var errors = ag.join();
    assertThat(executed[0]).isTrue();
    assertThat(errors).hasSize(1);

    assertThat(errors.get(0).index).isEqualTo(123);
    assertThat(errors.get(0).name).isEqualTo("foo");
    switch (errors.get(0).err.getReason()) {
      case FailToDoSomething r -> {}
      default -> fail();
    }
  }

  @Test
  void multiple_errors() {
    var ag = new AsyncGroupImpl();

    record Reason0() {}
    record Reason1() {}
    record Reason2() {}

    boolean[] executed = {false, false, false};

    Runner fn0 =
        () -> {
          try {
            Thread.sleep(200);
          } catch (Exception e) {
          }
          executed[0] = true;
          throw new Err(new Reason0());
        };
    Runner fn1 =
        () -> {
          try {
            Thread.sleep(400);
          } catch (Exception e) {
          }
          executed[1] = true;
          throw new Err(new Reason1());
        };
    Runner fn2 =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
          }
          executed[2] = true;
          throw new Err(new Reason2());
        };

    ag._index = 123;
    ag._name = "foo0";
    ag.add(fn0);
    ag._index = 456;
    ag._name = "foo1";
    ag.add(fn1);
    ag._index = 789;
    ag._name = "foo2";
    ag.add(fn2);

    var errors = ag.join();
    assertThat(executed[0]).isTrue();
    assertThat(executed[1]).isTrue();
    assertThat(executed[2]).isTrue();

    assertThat(errors.get(0).index).isEqualTo(789);
    assertThat(errors.get(0).name).isEqualTo("foo2");
    assertThat(errors.get(0).err.toString())
        .isEqualTo(
            "com.github.sttk.errs.Err { reason = com.github.sttk.sabi.internal.AsyncGroupImplTest$1Reason2 Reason2[], file = AsyncGroupImplTest.java, line = 114 }");
    assertThat(errors.get(1).index).isEqualTo(123);
    assertThat(errors.get(1).name).isEqualTo("foo0");
    assertThat(errors.get(1).err.toString())
        .isEqualTo(
            "com.github.sttk.errs.Err { reason = com.github.sttk.sabi.internal.AsyncGroupImplTest$1Reason0 Reason0[], file = AsyncGroupImplTest.java, line = 96 }");
    assertThat(errors.get(2).index).isEqualTo(456);
    assertThat(errors.get(2).name).isEqualTo("foo1");
    assertThat(errors.get(2).err.toString())
        .isEqualTo(
            "com.github.sttk.errs.Err { reason = com.github.sttk.sabi.internal.AsyncGroupImplTest$1Reason1 Reason1[], file = AsyncGroupImplTest.java, line = 105 }");
  }
}
