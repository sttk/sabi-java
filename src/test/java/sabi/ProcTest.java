package sabi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.ArrayList;

public class ProcTest {

  @Nested
  class Constructor {
    interface MyDax {
      String getData() throws Err;
      void setData(String data) throws Err;
    }
    class MyDaxImpl extends DaxBase implements MyDax {
      String greeting = "";
      public String getData() throws Err {
        return "world";
      }
      public void setData(String v) throws Err {
        greeting = "hello, " + v;
      }
    }

    @Test
    void should_create_an_instance() throws Err {
      var proc = new Proc<MyDax>(new MyDaxImpl());
      assertThat(proc).isNotNull();
    }

    class MyDaxImplButNotDaxBase implements MyDax {
      String greeting = "";
      public String getData() throws Err {
        return "world";
      }
      public void setData(String v) throws Err {
        greeting = "hello, " + v;
      }
    }

    @Test
    void should_throw_a_err_if_an_argument_is_not_DaxBase() throws Err {
      var dax = new MyDaxImplButNotDaxBase();
      try {
        new Proc<MyDax>(dax);
        fail();
      } catch (ClassCastException e) {
        assertThat(e.getMessage()).contains(
          "sabi.ProcTest$Constructor$MyDaxImplButNotDaxBase",
          "sabi.DaxBase"
        );
      }
    }
  }

  @Nested
  class RunTxn_without_DaxConn {

    interface MyDax {
      String getData() throws Err;
      void setData(String data) throws Err;
    }

    class MyDaxImpl extends DaxBase implements MyDax {
      String greeting = "";
      public String getData() throws Err {
        return "world";
      }
      public void setData(String v) throws Err {
        greeting = "hello, " + v;
      }
    }

    @Test
    void should_run_txn_with_runTxn() throws Exception {
      var daxImpl = new MyDaxImpl();
      var proc = new Proc<MyDax>(daxImpl);
      proc.runTxn(dax -> {
        var data = dax.getData();
        dax.setData(data);
      });
      assertThat(daxImpl.greeting).isEqualTo("hello, world");
    }

    @Test
    void should_run_txn_with_txn() throws Exception {
      var daxImpl = new MyDaxImpl();
      var proc = new Proc<MyDax>(daxImpl);
      var runner = proc.txn(dax -> {
        var data = dax.getData();
        dax.setData(data);
      });
      runner.run();
      assertThat(daxImpl.greeting).isEqualTo("hello, world");
    }
  }

  @Nested
  class RunTxn_and_txn_with_DaxConn {
    record FailToDoSomething() {};
    List<String> logs = new ArrayList<>();
    boolean willFailToDoSomething = false;

    class FooDaxConn implements DaxConn {
      @Override
      public void commit() throws Err {
        logs.add("FooDaxConn#commit");
      }
      @Override
      public void rollback() {
        logs.add("FooDaxConn#rollback");
      }
      @Override
      public void close() {
        logs.add("FooDaxConn#close");
      }
      public String fetchData() throws Err {
        return "world";
      }
      public boolean isError() {
        return willFailToDoSomething;
      }
    }
    class FooDaxSrc implements DaxSrc {
      @Override
      public DaxConn createDaxConn() throws Err {
        return new FooDaxConn();
      }
    }
    interface FooGetDax extends MyDax {
      DaxConn getDaxConn(String name) throws Err;
      default String getData() throws Err {
        var conn = FooDaxConn.class.cast(getDaxConn("foo"));
        if (conn.isError()) {
          throw new Err(new FailToDoSomething());
        }
        return conn.fetchData();
      }
    }

    class BarDaxConn implements DaxConn {
      BarDaxSrc ds;
      @Override
      public void commit() throws Err {
        logs.add("BarDaxConn#commit");
      }
      @Override
      public void rollback() {
        logs.add("BarDaxConn#rollback");
      }
      @Override
      public void close() {
        logs.add("BarDaxConn#close");
      }
      public void saveData(String data) throws Err {
        ds.data = data;
      }
    }
    class BarDaxSrc implements DaxSrc {
      String data = "";
      @Override
      public DaxConn createDaxConn() throws Err {
        var conn = new BarDaxConn();
        conn.ds = this;
        return conn;
      }
    }
    interface BarSetDax extends MyDax {
      DaxConn getDaxConn(String name) throws Err;
      default void setData(String data) throws Err {
        var conn = BarDaxConn.class.cast(getDaxConn("bar"));
        conn.saveData("hello, " + data);
      }
    }

    interface MyDax {
      String getData() throws Err;
      void setData(String data) throws Err;
    }
    class MyDaxImpl extends DaxBase implements MyDax,
      FooGetDax, BarSetDax {}

    @Test
    void should_run_and_commit_txn_with_runTxn() throws Err {
      var fooDs = new FooDaxSrc();
      var barDs = new BarDaxSrc();

      var daxImpl = new MyDaxImpl();
      var proc = new Proc<MyDax>(daxImpl);
      proc.addLocalDaxSrc("foo", fooDs);
      proc.addLocalDaxSrc("bar", barDs);

      proc.runTxn(dax -> {
        var data = dax.getData();
        dax.setData(data);
      });
      assertThat(barDs.data).isEqualTo("hello, world");
      assertThat(logs).containsExactly(
        "FooDaxConn#commit",
        "BarDaxConn#commit",
        "FooDaxConn#close",
        "BarDaxConn#close"
      );
    }

    @Test
    void should_run_and_rollback_txn_with_runTxn() throws Err {
      var fooDs = new FooDaxSrc();
      var barDs = new BarDaxSrc();

      var daxImpl = new MyDaxImpl();
      var proc = new Proc<MyDax>(daxImpl);
      proc.addLocalDaxSrc("foo", fooDs);
      proc.addLocalDaxSrc("bar", barDs);

      willFailToDoSomething = true;

      try {
        proc.runTxn(dax -> {
          var data = dax.getData();
          dax.setData(data);
        });
        fail();
      } catch (Err err) {
        assertThat(err.getReason()).isInstanceOf(FailToDoSomething.class);
      }
      assertThat(barDs.data).isEqualTo("");
      assertThat(logs).containsExactly(
        "FooDaxConn#rollback",
        "FooDaxConn#close"
      );
    }

    @Test
    void should_run_and_commit_txn_with_txn() throws Err {
      var fooDs = new FooDaxSrc();
      var barDs = new BarDaxSrc();

      var daxImpl = new MyDaxImpl();
      var proc = new Proc<MyDax>(daxImpl);
      proc.addLocalDaxSrc("foo", fooDs);
      proc.addLocalDaxSrc("bar", barDs);

      var runner = proc.txn(dax -> {
        var data = dax.getData();
        dax.setData(data);
      });

      runner.run();

      assertThat(barDs.data).isEqualTo("hello, world");
      assertThat(logs).containsExactly(
        "FooDaxConn#commit",
        "BarDaxConn#commit",
        "FooDaxConn#close",
        "BarDaxConn#close"
      );
    }

    @Test
    void should_run_and_rollback_txn_with_txn() throws Err {
      var fooDs = new FooDaxSrc();
      var barDs = new BarDaxSrc();

      var daxImpl = new MyDaxImpl();
      var proc = new Proc<MyDax>(daxImpl);
      proc.addLocalDaxSrc("foo", fooDs);
      proc.addLocalDaxSrc("bar", barDs);

      willFailToDoSomething = true;

      var runner = proc.txn(dax -> {
        var data = dax.getData();
        dax.setData(data);
      });

      try {
        runner.run();
        fail();
      } catch (Err err) {
        assertThat(err.getReason()).isInstanceOf(FailToDoSomething.class);
      }
      assertThat(barDs.data).isEqualTo("");
      assertThat(logs).containsExactly(
        "FooDaxConn#rollback",
        "FooDaxConn#close"
      );
    }
  }
}
