package com.github.sttk.sabi.errs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import java.io.IOException;

public class ErrTest {

  // error reasons
  record InvalidValue(String value) {}
  record FailToGetValue(String name) {}

  enum Reason {
    value,
    name,
  }

  @Test
  void should_create_an_Err() {
    try {
      throw new Err(new InvalidValue("abc"));
    } catch (Err e) {
      assertThat(e.getMessage()).isEqualTo("{reason=InvalidValue, value=abc}");
      assertThat(e.getReason()).isInstanceOf(InvalidValue.class);
      assertThat(e.getReasonName()).isEqualTo("InvalidValue");
      assertThat(e.getReasonPackage()).isEqualTo("com.github.sttk.sabi.errs");
      assertThat(e.get("value")).isEqualTo("abc");
      assertThat(e.get("name")).isNull();
      assertThat(e.get(Reason.value)).isEqualTo("abc");
      assertThat(e.get(Reason.name)).isNull();
      assertThat(e.getCause()).isNull();

      var m = e.getSituation();
      assertThat(m).hasSize(1);
      assertThat(m.get("value")).isEqualTo("abc");
    }
  }

  @Test
  void should_be_error_if_reason_is_null() {
    try {
      throw new Err(null);
    } catch (Err e) {
      fail(e);
    } catch (NullPointerException e) {
    } catch (Exception e) {
      fail(e);
    }

    var cause = new IOException();
    try {
      throw new Err(null, cause);
    } catch (Err e) {
      fail(e);
    } catch (NullPointerException e) {
    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  void should_create_an_Err_with_cause() {
    var cause = new IOException();
    try {
      throw new Err(new InvalidValue("abc"), cause);
    } catch (Err e) {
      assertThat(e.getMessage()).isEqualTo("{reason=InvalidValue, value=abc, cause=java.io.IOException}");
      assertThat(e.getReason()).isInstanceOf(InvalidValue.class);
      assertThat(e.getReasonName()).isEqualTo("InvalidValue");
      assertThat(e.getReasonPackage()).isEqualTo("com.github.sttk.sabi.errs");
      assertThat(e.get("value")).isEqualTo("abc");
      assertThat(e.get("name")).isNull();
      assertThat(e.get(Reason.value)).isEqualTo("abc");
      assertThat(e.get(Reason.name)).isNull();
      assertThat(e.getCause()).isEqualTo(cause);

      var m = e.getSituation();
      assertThat(m).hasSize(1);
      assertThat(e.get("value")).isEqualTo("abc");
    }
  }

  @Test
  void should_create_an_Err_with_cause_that_is_also_Err() {
    var cause = new Err(new FailToGetValue("foo"));
    try {
      throw new Err(new InvalidValue("abc"), cause);
    } catch (Err e) {
      assertThat(e.getMessage()).isEqualTo("{reason=InvalidValue, value=abc, cause={reason=FailToGetValue, name=foo}}");
      assertThat(e.getReason()).isInstanceOf(InvalidValue.class);
      assertThat(e.getReasonName()).isEqualTo("InvalidValue");
      assertThat(e.getReasonPackage()).isEqualTo("com.github.sttk.sabi.errs");
      assertThat(e.get("value")).isEqualTo("abc");
      assertThat(e.get("name")).isEqualTo("foo");
      assertThat(e.get(Reason.value)).isEqualTo("abc");
      assertThat(e.get(Reason.name)).isEqualTo("foo");
      assertThat(e.getCause()).isEqualTo(cause);

      var m = e.getSituation();
      assertThat(m).hasSize(2);
      assertThat(e.get("value")).isEqualTo("abc");
      assertThat(e.get("name")).isEqualTo("foo");

      assertThat(e.getFileName()).isEqualTo("ErrTest.java");
      assertThat(e.getLineNumber()).isEqualTo(88);
    }
  }
}
