package com.github.sttk.sabi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

import com.github.sttk.sabi.errs.Err;

import java.util.ArrayList;

public class SeqTest {

  record FailToDoSomething() {}

  @Test
  void should_run_argument_runners_sequencially() throws Exception {
    var logs = new ArrayList<String>();
    Seq.run(() -> logs.add("1"), () -> logs.add("2"));
    assertThat(logs).containsExactly("1", "2");
  }

  @Test
  void should_throw_an_Err_if_one_of_runners_causes_an_Err() {
    var logs = new ArrayList<String>();
    try {
      Seq.run(() -> {
        logs.add("1");
      }, () -> {
        throw new Err(new FailToDoSomething());
      });
    } catch (Err e) {
      assertThat(e.getReason()).isInstanceOf(FailToDoSomething.class);
    }
    assertThat(logs).containsExactly("1");
  }

  @Test
  void should_run_holding_runners_sequentially() throws Exception {
    var logs = new ArrayList<String>();
    var seq = new Seq(() -> logs.add("1"), () -> logs.add("2"));
    assertThat(logs).isEmpty();
    seq.run();
    assertThat(logs).containsExactly("1", "2");
  }
}
