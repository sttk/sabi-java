package com.github.sttk.sabi.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.Runner;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

public class AsyncGroupImplTest {
  private AsyncGroupImplTest() {}

  @Test
  void zero() {
    var ag = new AsyncGroupImpl();

    var m = new HashMap<String, Exc>();
    ag.joinAndPutExcsInto(m);
    assertThat(m).hasSize(0);
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

    ag.name = "foo";
    ag.add(fn);
    assertThat(executed[0]).isFalse();

    var m = new HashMap<String, Exc>();
    ag.joinAndPutExcsInto(m);
    assertThat(m).hasSize(0);
    assertThat(executed[0]).isTrue();
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
          throw new Exc(new FailToDoSomething());
        };

    ag.name = "foo";
    ag.add(fn);
    assertThat(executed[0]).isFalse();

    var m = new HashMap<String, Exc>();
    ag.joinAndPutExcsInto(m);
    assertThat(m).hasSize(1);
    assertThat(executed[0]).isTrue();

    switch (m.get("foo").getReason()) {
      case FailToDoSomething r -> {}
      default -> fail();
    }
  }

  @Test
  void multiple_errors_with_an_error_map() {
    var ag = new AsyncGroupImpl();

    record Reason0() {}
    record Reason1() {}
    record Reason2() {}

    boolean[] executed = {false, false, false};

    Runner fn0 =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
          }
          executed[0] = true;
          throw new Exc(new Reason0());
        };
    Runner fn1 =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
          }
          executed[1] = true;
          throw new Exc(new Reason1());
        };
    Runner fn2 =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
          }
          executed[2] = true;
          throw new Exc(new Reason2());
        };

    ag.name = "foo0";
    ag.add(fn0);
    ag.name = "foo1";
    ag.add(fn1);
    ag.name = "foo2";
    ag.add(fn2);

    var m = new HashMap<String, Exc>();
    ag.joinAndPutExcsInto(m);
    assertThat(m).hasSize(3);
    assertThat(executed[0]).isTrue();
    assertThat(executed[1]).isTrue();
    assertThat(executed[2]).isTrue();

    assertThat(m.get("foo0").toString())
        .isEqualTo(
            "com.github.sttk.errs.Exc { reason = com.github.sttk.sabi.internal.AsyncGroupImplTest$1Reason0 Reason0[], file = AsyncGroupImplTest.java, line = 96 }");
    assertThat(m.get("foo1").toString())
        .isEqualTo(
            "com.github.sttk.errs.Exc { reason = com.github.sttk.sabi.internal.AsyncGroupImplTest$1Reason1 Reason1[], file = AsyncGroupImplTest.java, line = 105 }");
    assertThat(m.get("foo2").toString())
        .isEqualTo(
            "com.github.sttk.errs.Exc { reason = com.github.sttk.sabi.internal.AsyncGroupImplTest$1Reason2 Reason2[], file = AsyncGroupImplTest.java, line = 114 }");
  }

  @Test
  void multiple_errors_without_an_error_map() {
    var ag = new AsyncGroupImpl();

    record Reason0() {}
    record Reason1() {}
    record Reason2() {}

    boolean[] executed = {false, false, false};

    Runner fn0 =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
          }
          executed[0] = true;
          throw new Exc(new Reason0());
        };
    Runner fn1 =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
          }
          executed[1] = true;
          throw new Exc(new Reason1());
        };
    Runner fn2 =
        () -> {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
          }
          executed[2] = true;
          throw new Exc(new Reason2());
        };

    ag.name = "foo0";
    ag.add(fn0);
    ag.name = "foo1";
    ag.add(fn1);
    ag.name = "foo2";
    ag.add(fn2);

    ag.joinAndIgnoreExcs();
    assertThat(executed[0]).isTrue();
    assertThat(executed[1]).isTrue();
    assertThat(executed[2]).isTrue();
  }
}
