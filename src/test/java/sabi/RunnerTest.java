package sabi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;

public class RunnerTest {

  record FailToDoSomething() {}

  @Nested
  class RunSeq {
    @Test
    void should_run_argument_runners_sequencially() throws Exception {
      var logs = new ArrayList<String>();
      Runner.runSeq(() -> logs.add("1"), () -> logs.add("2"));
      assertThat(logs).containsExactly("1", "2");
    }

    @Test
    void should_throw_an_Err_if_one_of_runners_cause_an_Err() {
      var logs = new ArrayList<String>();
      try {
        Runner.runSeq(() -> {
          logs.add("1");
        }, () -> {
          throw new Err(new FailToDoSomething());
        });
      } catch (Err e) {
        assertThat(e.getReason()).isInstanceOf(FailToDoSomething.class);
      }
      assertThat(logs).containsExactly("1");
    }
  }

  @Nested
  class RunPara {
    @Test
    void should_run_argument_runners_in_parallel() throws Exception {
      var logs = new ArrayList<String>();
      Runner.runPara(() -> {
        try { Thread.sleep(200); } catch (InterruptedException e) {}
        logs.add("1");
      }, () -> {
        try { Thread.sleep(20); } catch (InterruptedException e) {}
        logs.add("2");
      });
      assertThat(logs).containsExactly("2", "1");
    }

    @Test
    void should_throw_an_Err_if_one_of_runners_cause_an_Err() {
      var logs = new ArrayList<String>();
      try {
        Runner.runPara(() -> {
          throw new Err(new FailToDoSomething());
        }, () -> {
          logs.add("2");
        });
      } catch (Err e) {
        assertThat(e.getReason())
          .isInstanceOf(Runner.FailToRunInParallel.class);
        var reason = Runner.FailToRunInParallel.class.cast(e.getReason());
        assertThat(reason.errors()).hasSize(1);
        assertThat(reason.errors().get(0).getReason())
          .isInstanceOf(FailToDoSomething.class);
      }
      assertThat(logs).containsExactly("2");
    }

    @Test
    void should_throw_an_Err_if_one_of_runners_cause_an_RuntimeException() {
      var logs = new ArrayList<String>();
      try {
        Runner.runPara(() -> {
          throw new RuntimeException();
        }, () -> {
          logs.add("2");
        });
      } catch (Err e) {
        assertThat(e.getReason())
          .isInstanceOf(Runner.FailToRunInParallel.class);
        var reason = Runner.FailToRunInParallel.class.cast(e.getReason());
        assertThat(reason.errors()).hasSize(1);
        assertThat(reason.errors().get(0).getReason())
          .isInstanceOf(Runner.RunInParallelExceptionOccurs.class);
        assertThat(reason.errors().get(0).getCause())
          .isInstanceOf(RuntimeException.class);
      }
      assertThat(logs).containsExactly("2");
    }
  }

  @Nested
  class Seq {
    @Test
    void should_run_holding_runners_sequentially() throws Exception {
      var logs = new ArrayList<String>();
      var runner = Runner.seq(() -> logs.add("1"), () -> logs.add("2"));
      assertThat(logs).isEmpty();
      runner.run();
      assertThat(logs).containsExactly("1", "2");
    }
  }

  @Nested
  class Para {
    @Test
    void should_run_holding_runners_in_parallel() throws Exception {
      var logs = new ArrayList<String>();
      var runner = Runner.para(() -> {
        try { Thread.sleep(200); } catch (InterruptedException e) {}
        logs.add("1");
      }, () -> {
        try { Thread.sleep(20); } catch (InterruptedException e) {}
        logs.add("2");
      });
      assertThat(logs).isEmpty();
      runner.run();
      assertThat(logs).containsExactly("2", "1");
    }
  }
}
