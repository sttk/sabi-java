package com.github.sttk.sabi.internal;

import static com.github.sttk.sabi.AsyncGroup.Task;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
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
  void single_ok() {
    var ag = new AsyncGroupImpl();

    boolean[] executed = {false};
    Task task =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
          }
          executed[0] = true;
        };

    ag.index = 123;
    ag.name = "foo";
    ag.add(task);

    var errors = ag.join();
    assertThat(executed[0]).isTrue();
    assertThat(errors).isEmpty();
  }

  @Test
  void single_fail() {
    var ag = new AsyncGroupImpl();

    record FailToDoSomething() {}

    boolean[] executed = {false};
    Task task =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
          }
          executed[0] = true;
          throw new Err(new FailToDoSomething());
        };

    ag.index = 123;
    ag.name = "foo";
    ag.add(task);

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
  void multiple_ok() {
    var ag = new AsyncGroupImpl();

    boolean[] executed = {false, false, false};

    Task task0 =
        () -> {
          try {
            Thread.sleep(200);
          } catch (Exception e) {
          }
          executed[0] = true;
        };
    Task task1 =
        () -> {
          try {
            Thread.sleep(400);
          } catch (Exception e) {
          }
          executed[1] = true;
        };
    Task task2 =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
          }
          executed[2] = true;
        };

    ag.index = 123;
    ag.name = "foo";
    ag.add(task0);

    ag.index = 456;
    ag.name = "bar";
    ag.add(task1);

    ag.index = 789;
    ag.name = "baz";
    ag.add(task2);

    var errors = ag.join();
    assertThat(errors).hasSize(0);

    assertThat(executed[0]).isTrue();
    assertThat(executed[1]).isTrue();
    assertThat(executed[2]).isTrue();
  }

  @Test
  void multiple_process_and_single_fail() {
    var ag = new AsyncGroupImpl();

    record Reason0() {}
    record Reason1() {}
    record Reason2() {}

    boolean[] executed = {false, false, false};

    Task task0 =
        () -> {
          try {
            Thread.sleep(200);
          } catch (Exception e) {
          }
          executed[0] = true;
        };
    Task task1 =
        () -> {
          try {
            Thread.sleep(400);
          } catch (Exception e) {
          }
          executed[1] = true;
          throw new Err(new Reason1());
        };
    Task task2 =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
          }
          executed[2] = true;
        };

    ag.index = 123;
    ag.name = "foo";
    ag.add(task0);

    ag.index = 456;
    ag.name = "bar";
    ag.add(task1);

    ag.index = 789;
    ag.name = "baz";
    ag.add(task2);

    var errors = ag.join();
    assertThat(errors).hasSize(1);

    assertThat(errors.get(0).index).isEqualTo(456);
    assertThat(errors.get(0).name).isEqualTo("bar");
    assertThat(errors.get(0).err.toString())
        .isEqualTo(
            "com.github.sttk.errs.Err { reason = com.github.sttk.sabi.internal.AsyncGroupImplTest$1Reason1 Reason1[], file = AsyncGroupImplTest.java, line = 154 }");

    assertThat(executed[0]).isTrue();
    assertThat(executed[1]).isTrue();
    assertThat(executed[2]).isTrue();
  }

  @Test
  void multiple_fails() {
    var ag = new AsyncGroupImpl();

    record Reason0() {}
    record Reason1() {}
    record Reason2() {}

    boolean[] executed = {false, false, false};

    Task task0 =
        () -> {
          try {
            Thread.sleep(200);
          } catch (Exception e) {
          }
          executed[0] = true;
          throw new Err(new Reason0());
        };
    Task task1 =
        () -> {
          try {
            Thread.sleep(400);
          } catch (Exception e) {
          }
          executed[1] = true;
          throw new Err(new Reason1());
        };
    Task task2 =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
          }
          executed[2] = true;
          throw new Err(new Reason2());
        };

    ag.index = 123;
    ag.name = "foo";
    ag.add(task0);

    ag.index = 456;
    ag.name = "bar";
    ag.add(task1);

    ag.index = 789;
    ag.name = "baz";
    ag.add(task2);

    var errors = ag.join();
    assertThat(errors).hasSize(3);

    assertThat(executed[0]).isTrue();
    assertThat(executed[1]).isTrue();
    assertThat(executed[2]).isTrue();

    assertThat(errors.get(0).index).isEqualTo(789);
    assertThat(errors.get(0).name).isEqualTo("baz");
    assertThat(errors.get(0).err.toString())
        .isEqualTo(
            "com.github.sttk.errs.Err { reason = com.github.sttk.sabi.internal.AsyncGroupImplTest$2Reason2 Reason2[], file = AsyncGroupImplTest.java, line = 226 }");
    assertThat(errors.get(1).index).isEqualTo(123);
    assertThat(errors.get(1).name).isEqualTo("foo");
    assertThat(errors.get(1).err.toString())
        .isEqualTo(
            "com.github.sttk.errs.Err { reason = com.github.sttk.sabi.internal.AsyncGroupImplTest$2Reason0 Reason0[], file = AsyncGroupImplTest.java, line = 208 }");
    assertThat(errors.get(2).index).isEqualTo(456);
    assertThat(errors.get(2).name).isEqualTo("bar");
    assertThat(errors.get(2).err.toString())
        .isEqualTo(
            "com.github.sttk.errs.Err { reason = com.github.sttk.sabi.internal.AsyncGroupImplTest$2Reason1 Reason1[], file = AsyncGroupImplTest.java, line = 217 }");
  }

  @Test
  void run_but_a_RuntimeException_is_thrown() {
    var ag = new AsyncGroupImpl();

    boolean[] executed = {false};
    Task task =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
            fail(e);
          }
          executed[0] = true;
          throw new RuntimeException("runtime exception");
        };

    ag.index = 123;
    ag.name = "foo";
    ag.add(task);
    assertThat(executed[0]).isFalse();

    var errors = ag.join();
    assertThat(executed[0]).isTrue();
    assertThat(errors).hasSize(1);

    assertThat(errors.get(0).index).isEqualTo(123);
    assertThat(errors.get(0).name).isEqualTo("foo");
    switch (errors.get(0).err.getReason()) {
      case AsyncGroup.RuntimeExceptionOccurred r -> {}
      default -> fail();
    }
    assertThat(errors.get(0).err.getCause().getMessage()).isEqualTo("runtime exception");
    assertThat(errors.get(0).err.getCause()).isInstanceOf(RuntimeException.class);
  }

  @Test
  void run_but_a_Error_is_thrown() {
    var ag = new AsyncGroupImpl();

    boolean[] executed = {false};
    Task task =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
            fail(e);
          }
          executed[0] = true;
          throw new Error("error");
        };

    ag.index = 123;
    ag.name = "foo";
    ag.add(task);
    assertThat(executed[0]).isFalse();

    var errors = ag.join();
    assertThat(executed[0]).isTrue();
    assertThat(errors).hasSize(1);

    assertThat(errors.get(0).index).isEqualTo(123);
    assertThat(errors.get(0).name).isEqualTo("foo");
    switch (errors.get(0).err.getReason()) {
      case AsyncGroup.ErrorOccurred r -> {}
      default -> fail();
    }
    assertThat(errors.get(0).err.getCause().getMessage()).isEqualTo("error");
    assertThat(errors.get(0).err.getCause()).isInstanceOf(Error.class);
  }

  @Test
  void run_but_an_InterruptedException_is_thrown() {
    var ag = new AsyncGroupImpl();

    final var mainThread = Thread.currentThread();
    boolean[] executed = {false};
    Task task =
        () -> {
          try {
            Thread.sleep(500);
          } catch (Exception e) {
          }
          executed[0] = true;
          mainThread.interrupt();
        };

    ag.index = 123;
    ag.name = "foo";
    ag.add(task);
    assertThat(executed[0]).isFalse();

    var errors = ag.join();
    assertThat(errors).hasSize(1);

    assertThat(executed[0]).isTrue();
    assertThat(errors).hasSize(1);

    assertThat(errors.get(0).index).isEqualTo(123);
    assertThat(errors.get(0).name).isEqualTo("foo");
    switch (errors.get(0).err.getReason()) {
      case AsyncGroup.TaskInterrupted r -> {}
      default -> fail();
    }
    assertThat(errors.get(0).err.getCause()).isInstanceOf(InterruptedException.class);
  }
}
