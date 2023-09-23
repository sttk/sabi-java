package com.github.sttk.sabi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

import com.github.sttk.sabi.errs.Err;

import java.util.ArrayList;

public class ParaTest {

  record FailToDoSomething() {}

  @Test
  void should_run_argument_runners_in_parallel() throws Exception {
    var logs = new ArrayList<String>();
    Para.run(() -> {
      try { Thread.sleep(200); } catch (InterruptedException e) {}
      logs.add("1");
    }, () -> {
      try { Thread.sleep(20); } catch (InterruptedException e) {}
      logs.add("2");
    });
    assertThat(logs).containsExactly("2", "1");
  }

  @Test
  void should_throw_an_Err_if_one_of_runners_causes_an_Err() {
    var logs = new ArrayList<String>();
    try {
      Para.run(() -> {
        throw new Err(new FailToDoSomething());
      }, () -> {
        logs.add("2");
      });
    } catch (Err e) {
      var r = Para.FailToRunInParallel.class.cast(e.getReason());
      assertThat(r.errors()).hasSize(1);
      assertThat(r.errors().get(0).getReason())
        .isInstanceOf(FailToDoSomething.class);
    }
  }

  @Test
  void should_throw_an_Err_if_one_of_runners_causes_an_RuntimeException() {
    var logs = new ArrayList<String>();
    try {
      Para.run(() -> {
        throw new RuntimeException();
      }, () -> {
        logs.add("2");
      });
    } catch (Err e) {
      var r = Para.FailToRunInParallel.class.cast(e.getReason());
      assertThat(r.errors()).hasSize(1);
      assertThat(r.errors().get(0).getReason())
        .isInstanceOf(Para.RunInParallelExceptionOccurs.class);
      assertThat(r.errors().get(0).getCause())
        .isInstanceOf(RuntimeException.class);
    }
    assertThat(logs).containsExactly("2");
  }

  @Test
  void should_run_holding_runners_in_parallel() throws Exception {
    var logs = new ArrayList<String>();
    var para = new Para(() -> {
      try { Thread.sleep(200); } catch (InterruptedException e) {}
      logs.add("1");
    }, () -> {
      try { Thread.sleep(20); } catch (InterruptedException e) {}
      logs.add("2");
    });
    assertThat(logs).isEmpty();
    para.run();
    assertThat(logs).containsExactly("2", "1");
  }
}
