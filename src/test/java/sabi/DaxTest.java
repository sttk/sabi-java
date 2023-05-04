package sabi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static sabi.DaxAuxForTest.*;
import static sabi.DaxDummyForTest.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class DaxTest {

  @BeforeEach
  void clear() throws Exception {
    clearDaxBase();
  }

  @Nested
  class TestAddGlobalDaxSrc {
    @Test
    void should_add_DaxSrc() throws Exception {
      assertThat(isGlobalDaxSrcsFixed()).isFalse();
      assertThat(globalDaxSrcMap()).hasSize(0);

      DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());

      assertThat(isGlobalDaxSrcsFixed()).isFalse();
      assertThat(globalDaxSrcMap()).hasSize(1);

      DaxBase.addGlobalDaxSrc("bar", new BarDaxSrc());

      assertThat(isGlobalDaxSrcsFixed()).isFalse();
      assertThat(globalDaxSrcMap()).hasSize(2);
    }
  }

  @Nested
  class TestStartUpGlobalDaxSrcs {
    @Test
    void should_fix_composition_of_global_DaxSrc() throws Exception {
      assertThat(isGlobalDaxSrcsFixed()).isFalse();
      assertThat(globalDaxSrcMap()).hasSize(0);

      DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());

      assertThat(isGlobalDaxSrcsFixed()).isFalse();
      assertThat(globalDaxSrcMap()).hasSize(1);

      try {
        DaxBase.startUpGlobalDaxSrcs();
      } catch (Err e) {
        fail(e);
      }

      assertThat(isGlobalDaxSrcsFixed()).isTrue();
      assertThat(globalDaxSrcMap()).hasSize(1);

      DaxBase.addGlobalDaxSrc("bar", new BarDaxSrc());

      assertThat(isGlobalDaxSrcsFixed()).isTrue();
      assertThat(globalDaxSrcMap()).hasSize(1);

      assertThat(logs).hasSize(1);
      assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setUp");
    }

    @Test
    void should_fail_to_set_up_dax_src() throws Exception {
      willFailToSetUpFooDaxSrc = true;

      assertThat(isGlobalDaxSrcsFixed()).isFalse();
      assertThat(globalDaxSrcMap()).hasSize(0);

      DaxBase.addGlobalDaxSrc("bar", new BarDaxSrc());

      assertThat(isGlobalDaxSrcsFixed()).isFalse();
      assertThat(globalDaxSrcMap()).hasSize(1);

      DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());

      assertThat(isGlobalDaxSrcsFixed()).isFalse();
      assertThat(globalDaxSrcMap()).hasSize(2);

      try {
        DaxBase.startUpGlobalDaxSrcs();
        fail();
      } catch (Err e) {
        var r = DaxBase.FailToStartUpGlobalDaxSrcs.class.cast(e.getReason());
        var e1 = r.errors().get("foo");
        var r1 = FailToDoSomething.class.cast(e1.getReason());
        assertThat(r1.text()).isEqualTo("FailToSetUpFooDaxSrc");
      }

      assertThat(logs).hasSize(3);
      assertThat(logs.get(0)).isEqualTo("BarDaxSrc#setUp");
      if (logs.get(0).equals("FooDaxSrc#End")) {
        assertThat(logs.get(1)).isEqualTo("FooDaxSrc#end");
        assertThat(logs.get(2)).isEqualTo("BarDaxSrc#end");
      } else {
        assertThat(logs.get(1)).isEqualTo("BarDaxSrc#end");
        assertThat(logs.get(2)).isEqualTo("FooDaxSrc#end");
      }
    }
  }

  @Nested
  class TestShutdownGlobalDaxSrcs {
    @Test
    void should_free_all_global_dax_srcs() throws Exception {
      assertThat(isGlobalDaxSrcsFixed()).isFalse();
      assertThat(globalDaxSrcMap()).hasSize(0);

      DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());

      assertThat(isGlobalDaxSrcsFixed()).isFalse();
      assertThat(globalDaxSrcMap()).hasSize(1);

      DaxBase.addGlobalDaxSrc("bar", new BarDaxSrc());

      assertThat(isGlobalDaxSrcsFixed()).isFalse();
      assertThat(globalDaxSrcMap()).hasSize(2);

      try {
        DaxBase.startUpGlobalDaxSrcs();
      } catch (Err e) {
        fail(e);
      }

      assertThat(isGlobalDaxSrcsFixed()).isTrue();
      assertThat(globalDaxSrcMap()).hasSize(2);

      DaxBase.shutdownGlobalDaxSrcs();

      assertThat(isGlobalDaxSrcsFixed()).isTrue();
      assertThat(globalDaxSrcMap()).hasSize(2);

      assertThat(logs).hasSize(4);
      if (logs.get(0).equals("FooDaxSrc#setUp")) {
        assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setUp");
        assertThat(logs.get(1)).isEqualTo("BarDaxSrc#setUp");
      } else {
        assertThat(logs.get(0)).isEqualTo("BarDaxSrc#setUp");
        assertThat(logs.get(1)).isEqualTo("FooDaxSrc#setUp");
      }
      if (logs.get(2).equals("FooDaxSrc#end")) {
        assertThat(logs.get(2)).isEqualTo("FooDaxSrc#end");
        assertThat(logs.get(3)).isEqualTo("BarDaxSrc#end");
      } else {
        assertThat(logs.get(2)).isEqualTo("BarDaxSrc#end");
        assertThat(logs.get(3)).isEqualTo("FooDaxSrc#end");
      }
    }
  }

  @Nested
  class TestDaxBase {
    @Nested
    class TestSetUpLocalDaxSrc {
      @Test
      void should_register_local_dax_src() throws Exception {
        var base = new DaxBase() {};

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(0);
        assertThat(daxConnMap(base)).hasSize(0);

        base.setUpLocalDaxSrc("foo", new FooDaxSrc());

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(1);
        assertThat(daxConnMap(base)).hasSize(0);

        base.setUpLocalDaxSrc("bar", new BarDaxSrc());

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(2);
        assertThat(daxConnMap(base)).hasSize(0);

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setUp");
        assertThat(logs.get(1)).isEqualTo("BarDaxSrc#setUp");
      }

      @Test
      void should_unable_to_add_local_dax_src_in_txn() throws Exception {
        var base = new DaxBase() {};

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(0);
        assertThat(daxConnMap(base)).hasSize(0);

        try {
          base.setUpLocalDaxSrc("foo", new FooDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(1);
        assertThat(daxConnMap(base)).hasSize(0);

        base.begin();

        assertThat(isLocalDaxSrcsFixed(base)).isTrue();
        assertThat(localDaxSrcMap(base)).hasSize(1);
        assertThat(daxConnMap(base)).hasSize(0);

        try {
          base.setUpLocalDaxSrc("bar", new BarDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        assertThat(isLocalDaxSrcsFixed(base)).isTrue();
        assertThat(localDaxSrcMap(base)).hasSize(1);
        assertThat(daxConnMap(base)).hasSize(0);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setUp");

        base.end();

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(1);
        assertThat(daxConnMap(base)).hasSize(0);

        try {
          base.setUpLocalDaxSrc("bar", new BarDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(2);
        assertThat(daxConnMap(base)).hasSize(0);
      }

      @Test
      void should_fail_to_set_up_dax_src() throws Exception {
        willFailToSetUpFooDaxSrc = true;

        var base = new DaxBase() {};

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(0);
        assertThat(daxConnMap(base)).hasSize(0);

        try {
          base.setUpLocalDaxSrc("bar", new BarDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(1);
        assertThat(daxConnMap(base)).hasSize(0);

        try {
          base.setUpLocalDaxSrc("foo", new FooDaxSrc());
          fail();
        } catch (Err e) {
          var r = FailToDoSomething.class.cast(e.getReason());
          assertThat(r.text()).isEqualTo("FailToSetUpFooDaxSrc");
        }

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(1);
        assertThat(daxConnMap(base)).hasSize(0);

        base.freeAllLocalDaxSrcs();

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(0);
        assertThat(daxConnMap(base)).hasSize(0);

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0)).isEqualTo("BarDaxSrc#setUp");
        assertThat(logs.get(1)).isEqualTo("BarDaxSrc#end");
      }
    }

    @Nested
    class TestFreeLocalDaxSrc {
      @Test
      void should_free_local_dax_src() throws Exception {
        var base = new DaxBase() {};

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(0);
        assertThat(daxConnMap(base)).hasSize(0);

        try {
          base.setUpLocalDaxSrc("foo", new FooDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(1);
        assertThat(daxConnMap(base)).hasSize(0);

        base.freeLocalDaxSrc("foo");

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(0);
        assertThat(daxConnMap(base)).hasSize(0);

        try {
          base.setUpLocalDaxSrc("bar", new BarDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(1);
        assertThat(daxConnMap(base)).hasSize(0);

        try {
          base.setUpLocalDaxSrc("foo", new FooDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(2);
        assertThat(daxConnMap(base)).hasSize(0);

        base.freeLocalDaxSrc("bar");

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(1);
        assertThat(daxConnMap(base)).hasSize(0);

        base.freeLocalDaxSrc("foo");

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(0);
        assertThat(daxConnMap(base)).hasSize(0);

        assertThat(logs).hasSize(6);
        assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setUp");
        assertThat(logs.get(1)).isEqualTo("FooDaxSrc#end");
        assertThat(logs.get(2)).isEqualTo("BarDaxSrc#setUp");
        assertThat(logs.get(3)).isEqualTo("FooDaxSrc#setUp");
        assertThat(logs.get(4)).isEqualTo("BarDaxSrc#end");
        assertThat(logs.get(5)).isEqualTo("FooDaxSrc#end");
      }

      @Test
      void should_unable_to_free_local_dax_src_in_txn() throws Exception {
        var base = new DaxBase() {};

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(0);
        assertThat(daxConnMap(base)).hasSize(0);

        base.setUpLocalDaxSrc("foo", new FooDaxSrc());

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(1);
        assertThat(daxConnMap(base)).hasSize(0);

        base.begin();

        assertThat(isLocalDaxSrcsFixed(base)).isTrue();
        assertThat(localDaxSrcMap(base)).hasSize(1);
        assertThat(daxConnMap(base)).hasSize(0);

        base.freeLocalDaxSrc("foo");

        assertThat(isLocalDaxSrcsFixed(base)).isTrue();
        assertThat(localDaxSrcMap(base)).hasSize(1);
        assertThat(daxConnMap(base)).hasSize(0);

        base.end();

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(1);
        assertThat(daxConnMap(base)).hasSize(0);

        base.freeLocalDaxSrc("foo");

        assertThat(isLocalDaxSrcsFixed(base)).isFalse();
        assertThat(localDaxSrcMap(base)).hasSize(0);
        assertThat(daxConnMap(base)).hasSize(0);
      }
    }

    @Nested
    class TestGetDaxConn {
      @Test
      void should_get_dax_conn_of_local_dax_src() throws Exception {
        var base = new DaxBase() {};

        try {
          var conn = base.getDaxConn("foo");
          fail();
        } catch (Err e) {
          var r = DaxBase.DaxSrcIsNotFound.class.cast(e.getReason());
          assertThat(r.name()).isEqualTo("foo");
        }

        try {
          DaxBase.startUpGlobalDaxSrcs();
        } catch (Err e) {
          fail(e);
        }

        try {
          var conn = base.getDaxConn("foo");
          fail();
        } catch (Err e) {
          var r = DaxBase.DaxSrcIsNotFound.class.cast(e.getReason());
          assertThat(r.name()).isEqualTo("foo");
        }

        try {
          base.setUpLocalDaxSrc("foo", new FooDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        var conn = base.getDaxConn("foo");

        var conn2 = base.getDaxConn("foo");
        assertThat(conn2).isEqualTo(conn);
      }

      @Test
      void should_get_dax_conn_of_global_dax_src() throws Exception {
        var base = new DaxBase() {};

        try {
          var conn = base.getDaxConn("foo");
          fail();
        } catch (Err e) {
          var r = DaxBase.DaxSrcIsNotFound.class.cast(e.getReason());
          assertThat(r.name()).isEqualTo("foo");
        }

        DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());

        try {
          DaxBase.startUpGlobalDaxSrcs();
        } catch (Err e) {
          fail(e);
        }

        var conn = base.getDaxConn("foo");

        var conn2 = base.getDaxConn("foo");
        assertThat(conn2).isEqualTo(conn);
      }

      @Test
      void should_take_priority_of_local_ds_than_global_ds() throws Exception {
        var base = new DaxBase() {};

        try {
          var conn = base.getDaxConn("foo");
          fail();
        } catch (Err e) {
          var r = DaxBase.DaxSrcIsNotFound.class.cast(e.getReason());
          assertThat(r.name()).isEqualTo("foo");
        }

        DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());

        try {
          DaxBase.startUpGlobalDaxSrcs();
        } catch (Err e) {
          fail(e);
        }

        try {
          base.setUpLocalDaxSrc("foo", new FooDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        var conn = base.getDaxConn("foo");

        var conn2 = base.getDaxConn("foo");
        assertThat(conn2).isEqualTo(conn);
      }

      @Test
      void should_fail_to_create_dax_conn() throws Exception {
        willFailToCreateFooDaxConn = true;

        var base = new DaxBase() {};

        try {
          base.setUpLocalDaxSrc("foo", new FooDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        try {
          base.getDaxConn("foo");
          fail();
        } catch (Err e) {
          var r = DaxBase.FailToCreateDaxConn.class.cast(e.getReason());
          assertThat(r.name()).isEqualTo("foo");
          var e1 = Err.class.cast(e.getCause());
          assertThat(e1.getReason()).isInstanceOf(FailToDoSomething.class);
        }
      }

      @Test
      void should_commit() throws Exception {
        var base = new DaxBase() {};

        try {
          base.setUpLocalDaxSrc("foo", new FooDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        try {
          base.setUpLocalDaxSrc("bar", new BarDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        base.begin();

        try {
          var conn = base.getDaxConn("foo");
          assertThat(conn).isNotNull();
        } catch (Err e) {
          fail(e);
        }

        try {
          var conn = base.getDaxConn("bar");
          assertThat(conn).isNotNull();
        } catch (Err e) {
          fail(e);
        }

        try {
          base.commit();
        } catch (Err e) {
          fail(e);
        }

        assertThat(logs).hasSize(6);
        assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setUp");
        assertThat(logs.get(1)).isEqualTo("BarDaxSrc#setUp");
        assertThat(logs.get(2)).isEqualTo("FooDaxSrc#createDaxConn");
        assertThat(logs.get(3)).isEqualTo("BarDaxSrc#createDaxConn");
        if (logs.get(4).equals("FooDaxConn#commit")) {
          assertThat(logs.get(4)).isEqualTo("FooDaxConn#commit");
          assertThat(logs.get(5)).isEqualTo("BarDaxConn#commit");
        } else {
          assertThat(logs.get(4)).isEqualTo("BarDaxConn#commit");
          assertThat(logs.get(5)).isEqualTo("FooDaxConn#commit");
        }
      }

      @Test
      void should_fail_to_commit() throws Exception {
        willFailToCommitFooDaxConn = true;

        var base = new DaxBase() {};

        try {
          base.setUpLocalDaxSrc("foo", new FooDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        try {
          base.setUpLocalDaxSrc("bar", new BarDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        base.begin();

        try {
          var conn = base.getDaxConn("foo");
          assertThat(conn).isNotNull();
        } catch (Err e) {
          fail(e);
        }

        try {
          var conn = base.getDaxConn("bar");
          assertThat(conn).isNotNull();
        } catch (Err e) {
          fail(e);
        }

        try {
          base.commit();
          fail();
        } catch (Err e) {
          var r = DaxBase.FailToCommitDaxConn.class.cast(e.getReason());
          var e1 = r.errors().get("foo");
          var r1 = e1.getReason();
          assertThat(r1.getClass()).isEqualTo(FailToDoSomething.class);
        }

        assertThat(logs).hasSize(4);
        assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setUp");
        assertThat(logs.get(1)).isEqualTo("BarDaxSrc#setUp");
        assertThat(logs.get(2)).isEqualTo("FooDaxSrc#createDaxConn");
        assertThat(logs.get(3)).isEqualTo("BarDaxSrc#createDaxConn");
      }

      @Test
      void should_rollback() throws Exception {
        var base = new DaxBase() {};

        try {
          base.setUpLocalDaxSrc("foo", new FooDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        try {
          base.setUpLocalDaxSrc("bar", new BarDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        base.begin();

        try {
          var conn = base.getDaxConn("foo");
          assertThat(conn).isNotNull();
        } catch (Err e) {
          fail(e);
        }

        try {
          var conn = base.getDaxConn("bar");
          assertThat(conn).isNotNull();
        } catch (Err e) {
          fail(e);
        }

        base.rollback();

        assertThat(logs).hasSize(6);
        assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setUp");
        assertThat(logs.get(1)).isEqualTo("BarDaxSrc#setUp");
        assertThat(logs.get(2)).isEqualTo("FooDaxSrc#createDaxConn");
        assertThat(logs.get(3)).isEqualTo("BarDaxSrc#createDaxConn");
        if (logs.get(4).equals("FooDaxConn#rollback")) {
          assertThat(logs.get(4)).isEqualTo("FooDaxConn#rollback");
          assertThat(logs.get(5)).isEqualTo("BarDaxConn#rollback");
        } else {
          assertThat(logs.get(4)).isEqualTo("BarDaxConn#rollback");
          assertThat(logs.get(5)).isEqualTo("FooDaxConn#rollback");
        }
      }

      @Test
      void should_end() throws Exception {
        var base = new DaxBase() {};

        try {
          base.setUpLocalDaxSrc("foo", new FooDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        try {
          base.setUpLocalDaxSrc("bar", new BarDaxSrc());
        } catch (Err e) {
          fail(e);
        }

        base.begin();

        try {
          var conn = base.getDaxConn("foo");
          assertThat(conn).isNotNull();
        } catch (Err e) {
          fail(e);
        }

        try {
          var conn = base.getDaxConn("bar");
          assertThat(conn).isNotNull();
        } catch (Err e) {
          fail(e);
        }

        try {
          base.commit();
        } catch (Err e) {
          fail(e);
        }

        base.end();

        base.freeAllLocalDaxSrcs();

        assertThat(logs).hasSize(10);
        assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setUp");
        assertThat(logs.get(1)).isEqualTo("BarDaxSrc#setUp");
        assertThat(logs.get(2)).isEqualTo("FooDaxSrc#createDaxConn");
        assertThat(logs.get(3)).isEqualTo("BarDaxSrc#createDaxConn");
        if (logs.get(4).equals("FooDaxConn#commit")) {
          assertThat(logs.get(4)).isEqualTo("FooDaxConn#commit");
          assertThat(logs.get(5)).isEqualTo("BarDaxConn#commit");
        } else {
          assertThat(logs.get(4)).isEqualTo("BarDaxConn#commit");
          assertThat(logs.get(5)).isEqualTo("FooDaxConn#commit");
        }
        if (logs.get(6).equals("FooDaxConn#close")) {
          assertThat(logs.get(6)).isEqualTo("FooDaxConn#close");
          assertThat(logs.get(7)).isEqualTo("BarDaxConn#close");
        } else {
          assertThat(logs.get(6)).isEqualTo("BarDaxConn#close");
          assertThat(logs.get(7)).isEqualTo("FooDaxConn#close");
        }
        if (logs.get(8).equals("FooDaxSrc#end")) {
          assertThat(logs.get(8)).isEqualTo("FooDaxSrc#end");
          assertThat(logs.get(9)).isEqualTo("BarDaxSrc#end");
        } else {
          assertThat(logs.get(8)).isEqualTo("BarDaxSrc#end");
          assertThat(logs.get(9)).isEqualTo("FooDaxSrc#end");
        }
      }
    }
  }

  @Nested
  class TestDax {
    @Test
    void should_run_txn() throws Exception {
      var hogeDs = new MapDaxSrc();
      var fugaDs = new MapDaxSrc();
      var piyoDs = new MapDaxSrc();

      var base = new HogeFugaPiyoDaxBase();

      try {
        base.setUpLocalDaxSrc("hoge", hogeDs);
        base.setUpLocalDaxSrc("fuga", fugaDs);
        base.setUpLocalDaxSrc("piyo", piyoDs);

        hogeDs.dataMap.put("hogehoge", "Hello, world");

        try {
          Txn.run(base, new HogeFugaLogic());
        } catch (Err e) {
          fail(e);
        }

        try {
          Txn.run(base, new FugaPiyoLogic());
        } catch (Err e) {
          fail(e);
        }

        assertThat(piyoDs.dataMap.get("piyopiyo")).isEqualTo("Hello, world");

      } catch (Err e) {
        fail(e);
      } finally {
        base.freeAllLocalDaxSrcs();
      }
    }
  }
}
