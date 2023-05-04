package sabi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import java.io.IOException;

public class ErrTest {

  // error reasons
  record FailToDoSomething() {}
  record InvalidState(String name1) {}
  record InvalidValue(String name2, int name3) {}

  @Test
  void should_create_an_exception_with_reason() {
    try {
      throw new Err(new FailToDoSomething());
    } catch (Err e) {
      assertThat(e.getReason()).isInstanceOf(FailToDoSomething.class);
      assertThat(e.getSituation()).isEmpty();
      assertThat(e.get("name1")).isNull();
      assertThat(e.get("name2")).isNull();
      assertThat(e.get("name3")).isNull();
      assertThat(e.getCause()).isNull();
      assertThat(e.getMessage()).isEqualTo("{reason=FailToDoSomething}");
      assertThat(e.getClassName()).isEqualTo("sabi.ErrTest");
      assertThat(e.getMethodName()).isEqualTo(
        "should_create_an_exception_with_reason");
      assertThat(e.getFileName()).isEqualTo("ErrTest.java");
      assertThat(e.getLineNumber()).isEqualTo(18);
    }
  }

  @Test
  void should_create_an_exception_with_reason_and_cause() {
    final var cause = new IOException("Message");
    try {
      throw new Err(new FailToDoSomething(), cause);
    } catch (Err e) {
      assertThat(e.getReason()).isInstanceOf(FailToDoSomething.class);
      assertThat(e.getSituation()).isEmpty();
      assertThat(e.get("name1")).isNull();
      assertThat(e.get("name2")).isNull();
      assertThat(e.get("name3")).isNull();
      assertThat(e.getCause()).isEqualTo(cause);
      assertThat(e.getMessage()).isEqualTo(
        "{reason=FailToDoSomething, cause=java.io.IOException: Message}");
      assertThat(e.getClassName()).isEqualTo("sabi.ErrTest");
      assertThat(e.getMethodName()).isEqualTo(
        "should_create_an_exception_with_reason_and_cause");
      assertThat(e.getFileName()).isEqualTo("ErrTest.java");
      assertThat(e.getLineNumber()).isEqualTo(39);
    }
  }

  enum Param {
    name1,
    name2,
    name3,
  }

  @Test
  void should_create_an_exception_with_reason_and_params() {
    try {
      throw new Err(new InvalidState("abc"));
    } catch (Err e) {
      assertThat(e.getReason()).isInstanceOf(InvalidState.class);
      assertThat(e.getSituation()).hasSize(1)
        .containsEntry("name1", "abc");
      assertThat(e.get(Param.name1)).isEqualTo("abc");
      assertThat(e.get(Param.name2)).isNull();
      assertThat(e.get(Param.name3)).isNull();
      assertThat(e.getCause()).isNull();
      assertThat(e.getMessage()).isEqualTo("{reason=InvalidState, name1=abc}");
      assertThat(e.getClassName()).isEqualTo("sabi.ErrTest");
      assertThat(e.getMethodName()).isEqualTo(
        "should_create_an_exception_with_reason_and_params");
      assertThat(e.getFileName()).isEqualTo("ErrTest.java");
      assertThat(e.getLineNumber()).isEqualTo(66);
    }
  }

  @Test
  void should_create_an_exception_with_reason_and_cause_and_params() {
    var cause = new IOException("Message");
    try {
      throw new Err(new InvalidValue("abc", 123), cause);
    } catch (Err e) {
      assertThat(e.getReason()).isInstanceOf(InvalidValue.class);
      assertThat(e.getSituation()).hasSize(2)
        .containsEntry("name2", "abc")
        .containsEntry("name3", 123);
      assertThat(e.get("name1")).isNull();
      assertThat(e.get("name2")).isEqualTo("abc");
      assertThat(e.get("name3")).isEqualTo(123);
      assertThat(e.getCause()).isEqualTo(cause);
      assertThat(e.getMessage()).isEqualTo(
        "{reason=InvalidValue, name2=abc, name3=123, " +
        "cause=java.io.IOException: Message}");
      assertThat(e.getClassName()).isEqualTo("sabi.ErrTest");
      assertThat(e.getMethodName()).isEqualTo(
        "should_create_an_exception_with_reason_and_cause_and_params");
      assertThat(e.getFileName()).isEqualTo("ErrTest.java");
      assertThat(e.getLineNumber()).isEqualTo(88);
    }
  }

  @Test
  void should_print_message_when_cause_is_Err() {
    final var cause0 = new IOException("Message");
    try {
      throw new Err(new InvalidValue("abc", 123), cause0);
    } catch (Err cause1) {
      try {
        throw new Err(new InvalidState("def"), cause1);
      } catch (Err e) {
        assertThat(e.getReason()).isInstanceOf(InvalidState.class);
        assertThat(e.getSituation()).hasSize(3)
          .containsEntry("name1", "def")
          .containsEntry("name2", "abc")
          .containsEntry("name3", 123);
        assertThat(e.get("name1")).isEqualTo("def");
        assertThat(e.get("name2")).isEqualTo("abc");
        assertThat(e.get("name3")).isEqualTo(123);
        assertThat(e.getCause()).isEqualTo(cause1);
        assertThat(e.getMessage()).isEqualTo(
          "{reason=InvalidState, name1=def, cause=" +
          "{reason=InvalidValue, name2=abc, name3=123, cause=" +
          "java.io.IOException: Message}}");
        assertThat(e.getClassName()).isEqualTo("sabi.ErrTest");
        assertThat(e.getMethodName()).isEqualTo(
          "should_print_message_when_cause_is_Err");
        assertThat(e.getFileName()).isEqualTo("ErrTest.java");
        assertThat(e.getLineNumber()).isEqualTo(116);
      }
    }
  }

  @Test
  void should_throw_NPE_if_reason_is_null() {
    try {
      new Err(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).isNotNull();
    }
  }

  @Test
  void should_throw_NPE_if_reason_is_null_and_with_cause() {
    var cause = new IOException("Message");
    try {
      new Err(null, cause);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).isNotNull();
    }
  }
}
