package sabi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class DaxBaseTest {

  final List<String> logs = new ArrayList<>();
  boolean willFailToCreateFooDaxConn = false;
  boolean willFailToCommitFooDaxConn = false;
  boolean willThrowCommitExceptionOccurs = false;

  final record InvalidDaxConn() {}

  class FooDaxConn implements DaxConn {
    String label;
    public FooDaxConn() {
      this.label = "";
    }
    public FooDaxConn(final String label) {
      this.label = label;
    }
    public void commit() throws Err {
      if (willFailToCommitFooDaxConn) {
        throw new Err(new InvalidDaxConn());
      }
      if (willThrowCommitExceptionOccurs) {
        throw new RuntimeException();
      }
      logs.add("FooDaxConn#commit");
    }
    public void rollback() {
      logs.add("FooDaxConn#rollback");
    }
    public void close() {
      logs.add("FooDaxConn#close");
    }
  }

  class FooDaxSrc implements DaxSrc {
    String label;
    public FooDaxSrc() {
      this.label = "";
    }
    public FooDaxSrc(String label) {
      this.label = label;
    }
    public DaxConn createDaxConn() throws Err {
      if (willFailToCreateFooDaxConn) {
        throw new Err(new InvalidDaxConn());
      }
      return new FooDaxConn(this.label);
    }
  }

  class BarDaxConn implements DaxConn {
    String label;
    public BarDaxConn() {
      this.label = "";
    }
    public BarDaxConn(final String label) {
      this.label = label;
    }
    public void commit() throws Err {
      logs.add("BarDaxConn#commit");
    }
    public void rollback() {
      logs.add("BarDaxConn#rollback");
    }
    public void close() {
      logs.add("BarDaxConn#close");
    }
  }

  class BarDaxSrc implements DaxSrc {
    String label;
    public BarDaxSrc() {
      this.label = "";
    }
    public BarDaxSrc(String label) {
      this.label = label;
    }
    public DaxConn createDaxConn() throws Err {
      return new BarDaxConn(this.label);
    }
  }


  @BeforeEach
  void clear() throws Exception {
    final var f0 = DaxBase.class.getDeclaredField("isGlobalDaxSrcsFixed");
    f0.setAccessible(true);
    f0.setBoolean(null, false);

    final var f1 = DaxBase.class.getDeclaredField("globalDaxSrcMap");
    f1.setAccessible(true);
    @SuppressWarnings("unchecked")
    var map1 = (Map<String, DaxSrc>) f1.get(null);
    map1.clear();

    willFailToCreateFooDaxConn = false;
    willFailToCommitFooDaxConn = false;
  }

  @Nested
  class AddGlobalDaxSrc {
    @Test
    void should_add_DaxSrc() throws Exception {
      final var f0 = DaxBase.class.getDeclaredField("isGlobalDaxSrcsFixed");
      f0.setAccessible(true);
      assertThat(f0.getBoolean(null)).isFalse();

      final var f1 = DaxBase.class.getDeclaredField("globalDaxSrcMap");
      f1.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map1 = (Map<String, DaxSrc>) f1.get(null);
      assertThat(map1).isEmpty();

      DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());

      assertThat(f0.getBoolean(null)).isFalse();
      assertThat(map1).hasSize(1);

      DaxBase.addGlobalDaxSrc("bar", new BarDaxSrc());

      assertThat(f0.getBoolean(null)).isFalse();
      assertThat(map1).hasSize(2);
    }
  }

  @Nested
  class FixGlobalDaxSrcs {
    @Test
    void should_fix_composition_of_global_DaxSrc() throws Exception {
      final var f0 = DaxBase.class.getDeclaredField("isGlobalDaxSrcsFixed");
      f0.setAccessible(true);
      assertThat(f0.getBoolean(null)).isFalse();

      final var f1 = DaxBase.class.getDeclaredField("globalDaxSrcMap");
      f1.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map1 = (Map<String, DaxSrc>) f1.get(null);
      assertThat(map1).isEmpty();

      DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());

      assertThat(f0.getBoolean(null)).isFalse();
      assertThat(map1).hasSize(1);

      DaxBase.fixGlobalDaxSrcs();

      assertThat(f0.getBoolean(null)).isTrue();
      assertThat(map1).hasSize(1);

      DaxBase.addGlobalDaxSrc("bar", new BarDaxSrc());

      assertThat(f0.getBoolean(null)).isTrue();
      assertThat(map1).hasSize(1);

      f0.setBoolean(null, false);

      assertThat(f0.getBoolean(null)).isFalse();
      assertThat(map1).hasSize(1);

      DaxBase.addGlobalDaxSrc("bar", new BarDaxSrc());

      assertThat(f0.getBoolean(null)).isFalse();
      assertThat(map1).hasSize(2);
    }
  }

  @Nested
  class AddLocalDaxSrc {
    @Test
    void should_add_DaxSrc() throws Exception {
      var base = new DaxBase() {};

      final var f0 = DaxBase.class.getDeclaredField("isLocalDaxSrcsFixed");
      f0.setAccessible(true);
      assertThat(f0.getBoolean(base)).isFalse();

      final var f1 = DaxBase.class.getDeclaredField("localDaxSrcMap");
      f1.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map1 = (Map<String, DaxSrc>) f1.get(base);
      assertThat(map1).isEmpty();

      final var f2 = DaxBase.class.getDeclaredField("daxConnMap");
      f2.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map2 = (Map<String, DaxConn>) f2.get(base);
      assertThat(map2).isEmpty();

      base.addLocalDaxSrc("foo", new FooDaxSrc());

      assertThat(f0.getBoolean(base)).isFalse();
      assertThat(map1).hasSize(1);
      assertThat(map2).isEmpty();

      base.addLocalDaxSrc("bar", new BarDaxSrc());

      assertThat(f0.getBoolean(base)).isFalse();
      assertThat(map1).hasSize(2);
      assertThat(map2).isEmpty();
    }
  }

  @Nested
  class Begin {
    @Test
    void should_fix_composition_of_global_and_local_DaxSrc() throws Exception {
      var base = new DaxBase() {};

      final var f0 = DaxBase.class.getDeclaredField("isGlobalDaxSrcsFixed");
      f0.setAccessible(true);
      assertThat(f0.getBoolean(null)).isFalse();

      final var f1 = DaxBase.class.getDeclaredField("globalDaxSrcMap");
      f1.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map1 = (Map<String, DaxSrc>) f1.get(null);
      assertThat(map1).isEmpty();

      final var f2 = DaxBase.class.getDeclaredField("isLocalDaxSrcsFixed");
      f2.setAccessible(true);
      assertThat(f2.getBoolean(base)).isFalse();

      final var f3 = DaxBase.class.getDeclaredField("localDaxSrcMap");
      f3.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map3 = (Map<String, DaxSrc>) f3.get(base);
      assertThat(map3).isEmpty();

      final var f4 = DaxBase.class.getDeclaredField("daxConnMap");
      f4.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map4 = (Map<String, DaxConn>) f4.get(base);
      assertThat(map4).isEmpty();

      DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());
      base.addLocalDaxSrc("foo", new FooDaxSrc());

      assertThat(f0.getBoolean(null)).isFalse();
      assertThat(map1).hasSize(1);
      assertThat(f2.getBoolean(base)).isFalse();
      assertThat(map3).hasSize(1);
      assertThat(map4).isEmpty();

      base.begin();

      assertThat(f0.getBoolean(null)).isTrue();
      assertThat(map1).hasSize(1);
      assertThat(f2.getBoolean(base)).isTrue();
      assertThat(map3).hasSize(1);
      assertThat(map4).isEmpty();

      DaxBase.addGlobalDaxSrc("bar", new BarDaxSrc());
      base.addLocalDaxSrc("bar", new BarDaxSrc());

      assertThat(f0.getBoolean(null)).isTrue();
      assertThat(map1).hasSize(1);
      assertThat(f2.getBoolean(base)).isTrue();
      assertThat(map3).hasSize(1);
      assertThat(map4).isEmpty();

      f2.setBoolean(base, false);

      assertThat(f0.getBoolean(null)).isTrue();
      assertThat(map1).hasSize(1);
      assertThat(f2.getBoolean(base)).isFalse();
      assertThat(map3).hasSize(1);
      assertThat(map4).isEmpty();

      DaxBase.addGlobalDaxSrc("bar", new BarDaxSrc());
      base.addLocalDaxSrc("bar", new BarDaxSrc());

      assertThat(f0.getBoolean(null)).isTrue();
      assertThat(map1).hasSize(1);
      assertThat(f2.getBoolean(base)).isFalse();
      assertThat(map3).hasSize(2);
      assertThat(map4).isEmpty();

      f0.setBoolean(null, false);

      assertThat(f0.getBoolean(null)).isFalse();
      assertThat(map1).hasSize(1);
      assertThat(f2.getBoolean(base)).isFalse();
      assertThat(map3).hasSize(2);
      assertThat(map4).isEmpty();

      DaxBase.addGlobalDaxSrc("bar", new BarDaxSrc());

      assertThat(f0.getBoolean(null)).isFalse();
      assertThat(map1).hasSize(2);
      assertThat(f2.getBoolean(base)).isFalse();
      assertThat(map3).hasSize(2);
      assertThat(map4).isEmpty();
    }
  }

  @Nested
  class GetDaxConn {
    @Test
    void should_get_DaxConn_with_local_DaxSrc() throws Exception {
      var base = new DaxBase() {};

      final var f0 = DaxBase.class.getDeclaredField("isGlobalDaxSrcsFixed");
      f0.setAccessible(true);
      assertThat(f0.getBoolean(null)).isFalse();

      final var f1 = DaxBase.class.getDeclaredField("globalDaxSrcMap");
      f1.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map1 = (Map<String, DaxSrc>) f1.get(null);
      assertThat(map1).isEmpty();

      final var f2 = DaxBase.class.getDeclaredField("isLocalDaxSrcsFixed");
      f2.setAccessible(true);
      assertThat(f2.getBoolean(base)).isFalse();

      final var f3 = DaxBase.class.getDeclaredField("localDaxSrcMap");
      f3.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map3 = (Map<String, DaxSrc>) f3.get(base);
      assertThat(map3).isEmpty();

      final var f4 = DaxBase.class.getDeclaredField("daxConnMap");
      f4.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map4 = (Map<String, DaxConn>) f4.get(base);
      assertThat(map4).isEmpty();

      try {
        base.getDaxConn("foo");
        fail();
      } catch (Err e) {
        var reason = DaxBase.DaxSrcIsNotFound.class.cast(e.getReason());
        assertThat(reason.name()).isEqualTo("foo");
      }

      base.addLocalDaxSrc("foo", new FooDaxSrc());

      assertThat(f0.getBoolean(null)).isFalse();
      assertThat(map1).hasSize(0);
      assertThat(f2.getBoolean(base)).isFalse();
      assertThat(map3).hasSize(1);
      assertThat(map4).isEmpty();

      final var conn = base.getDaxConn("foo");
      assertThat(conn).isInstanceOf(FooDaxConn.class);

      final var conn1 = base.getDaxConn("foo");
      assertThat(conn1).isEqualTo(conn);
    }

    @Test
    void should_get_DaxConn_with_global_DaxSrc() throws Exception {
      var base = new DaxBase() {};

      final var f0 = DaxBase.class.getDeclaredField("isGlobalDaxSrcsFixed");
      f0.setAccessible(true);
      assertThat(f0.getBoolean(null)).isFalse();

      final var f1 = DaxBase.class.getDeclaredField("globalDaxSrcMap");
      f1.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map1 = (Map<String, DaxSrc>) f1.get(null);
      assertThat(map1).isEmpty();

      final var f2 = DaxBase.class.getDeclaredField("isLocalDaxSrcsFixed");
      f2.setAccessible(true);
      assertThat(f2.getBoolean(base)).isFalse();

      final var f3 = DaxBase.class.getDeclaredField("localDaxSrcMap");
      f3.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map3 = (Map<String, DaxSrc>) f3.get(base);
      assertThat(map3).isEmpty();

      final var f4 = DaxBase.class.getDeclaredField("daxConnMap");
      f4.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map4 = (Map<String, DaxConn>) f4.get(base);
      assertThat(map4).isEmpty();

      try {
        base.getDaxConn("foo");
        fail();
      } catch (Err e) {
        var reason = DaxBase.DaxSrcIsNotFound.class.cast(e.getReason());
        assertThat(reason.name()).isEqualTo("foo");
      }

      DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());

      assertThat(f0.getBoolean(null)).isFalse();
      assertThat(map1).hasSize(1);
      assertThat(f2.getBoolean(base)).isFalse();
      assertThat(map3).hasSize(0);
      assertThat(map4).isEmpty();

      final var conn = base.getDaxConn("foo");
      assertThat(conn).isInstanceOf(FooDaxConn.class);

      final var conn1 = base.getDaxConn("foo");
      assertThat(conn1).isEqualTo(conn);
    }

    @Test
    void should_take_local_DaxSrc_priority_of_global_DaxSrc_with_same_name() throws Exception {
      var base = new DaxBase() {};

      final var f0 = DaxBase.class.getDeclaredField("isGlobalDaxSrcsFixed");
      f0.setAccessible(true);
      assertThat(f0.getBoolean(null)).isFalse();

      final var f1 = DaxBase.class.getDeclaredField("globalDaxSrcMap");
      f1.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map1 = (Map<String, DaxSrc>) f1.get(null);
      assertThat(map1).isEmpty();

      final var f2 = DaxBase.class.getDeclaredField("isLocalDaxSrcsFixed");
      f2.setAccessible(true);
      assertThat(f2.getBoolean(base)).isFalse();

      final var f3 = DaxBase.class.getDeclaredField("localDaxSrcMap");
      f3.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map3 = (Map<String, DaxSrc>) f3.get(base);
      assertThat(map3).isEmpty();

      final var f4 = DaxBase.class.getDeclaredField("daxConnMap");
      f4.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map4 = (Map<String, DaxConn>) f4.get(base);
      assertThat(map4).isEmpty();

      DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc("global"));
      base.addLocalDaxSrc("foo", new FooDaxSrc("local"));

      assertThat(f0.getBoolean(null)).isFalse();
      assertThat(map1).hasSize(1);
      assertThat(f2.getBoolean(base)).isFalse();
      assertThat(map3).hasSize(1);
      assertThat(map4).isEmpty();

      final var conn = base.getDaxConn("foo");

      assertThat(conn).isInstanceOf(FooDaxConn.class);
      assertThat(FooDaxConn.class.cast(conn).label).isEqualTo("local");
    }

    @Test
    void should_throw_Err_if_failing_to_create_DaxConn() throws Exception {
      var base = new DaxBase() {};

      final var f0 = DaxBase.class.getDeclaredField("isGlobalDaxSrcsFixed");
      f0.setAccessible(true);
      assertThat(f0.getBoolean(null)).isFalse();

      final var f1 = DaxBase.class.getDeclaredField("globalDaxSrcMap");
      f1.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map1 = (Map<String, DaxSrc>) f1.get(null);
      assertThat(map1).isEmpty();

      final var f2 = DaxBase.class.getDeclaredField("isLocalDaxSrcsFixed");
      f2.setAccessible(true);
      assertThat(f2.getBoolean(base)).isFalse();

      final var f3 = DaxBase.class.getDeclaredField("localDaxSrcMap");
      f3.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map3 = (Map<String, DaxSrc>) f3.get(base);
      assertThat(map3).isEmpty();

      final var f4 = DaxBase.class.getDeclaredField("daxConnMap");
      f4.setAccessible(true);
      @SuppressWarnings("unchecked")
      var map4 = (Map<String, DaxConn>) f4.get(base);
      assertThat(map4).isEmpty();

      base.addLocalDaxSrc("foo", new FooDaxSrc("local"));

      assertThat(f0.getBoolean(null)).isFalse();
      assertThat(map1).hasSize(0);
      assertThat(f2.getBoolean(base)).isFalse();
      assertThat(map3).hasSize(1);
      assertThat(map4).isEmpty();

      willFailToCreateFooDaxConn = true;

      try {
        base.getDaxConn("foo");
        fail();
      } catch (Err e) {
        var reason = DaxBase.FailToCreateDaxConn.class.cast(e.getReason());
        assertThat(reason.name()).isEqualTo("foo");
        assertThat(e.get("name")).isEqualTo("foo");
        assertThat(e.getCause().toString()).isEqualTo("sabi.Err: {reason=InvalidDaxConn}");
      }
    }
  }

  @Nested
  class Commit {
    @Test
    void should_commit() throws Exception {
      var base = new DaxBase() {};
      DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());
      base.addLocalDaxSrc("bar", new BarDaxSrc());

      base.getDaxConn("foo");
      base.getDaxConn("bar");
      base.commit();

      assertThat(logs).containsOnly(
        "FooDaxConn#commit",
        "BarDaxConn#commit"
      );
    }

    @Test
    void should_throw_Err_if_failing_to_commit() throws Exception {
      var base = new DaxBase() {};

      DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());
      base.addLocalDaxSrc("bar", new BarDaxSrc());

      willFailToCommitFooDaxConn = true;

      base.getDaxConn("foo");
      base.getDaxConn("bar");

      try {
        base.commit();
        fail();
      } catch (Err e) {
        var reason = DaxBase.FailToCommitDaxConn.class.cast(e.getReason());
        @SuppressWarnings("unchecked")
        var errs = (Map<String,Err>) reason.errors();
        assertThat(errs).hasSize(1);
        assertThat(errs.get("foo").toString()).isEqualTo("sabi.Err: {reason=InvalidDaxConn}");
      }

      assertThat(logs).isEmpty();
    }

    @Test
    void should_throw_Err_if_runtime_exception_occurs() throws Exception {
      var base = new DaxBase() {};

      DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());
      base.addLocalDaxSrc("bar", new BarDaxSrc());

      willThrowCommitExceptionOccurs = true;

      base.getDaxConn("foo");
      base.getDaxConn("bar");

      try {
        base.commit();
        fail();
      } catch (Err e) {
        var reason = DaxBase.FailToCommitDaxConn.class.cast(e.getReason());
        @SuppressWarnings("unchecked")
        var errs = (Map<String,Err>) reason.errors();
        assertThat(errs).hasSize(1);
        assertThat(errs.get("foo").toString()).isEqualTo("sabi.Err: {reason=CommitExceptionOccurs, name=foo, cause=java.lang.RuntimeException}");
      }

      assertThat(logs).isEmpty();
    }
  }

  @Nested
  class Rollback {
    @Test
    void should_rollback() throws Exception {
      var base = new DaxBase() {};

      DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());
      base.addLocalDaxSrc("bar", new BarDaxSrc());

      willFailToCommitFooDaxConn = true;

      base.getDaxConn("foo");
      base.getDaxConn("bar");

      base.rollback();

      assertThat(logs).containsOnly(
        "FooDaxConn#rollback",
        "BarDaxConn#rollback"
      );
    }
  }

  @Nested
  class Close {
    @Test
    void should_close() throws Exception {
      var base = new DaxBase() {};

      DaxBase.addGlobalDaxSrc("foo", new FooDaxSrc());
      base.addLocalDaxSrc("bar", new BarDaxSrc());

      willFailToCommitFooDaxConn = true;

      base.getDaxConn("foo");
      base.getDaxConn("bar");

      base.close();

      assertThat(logs).containsOnly(
        "FooDaxConn#close",
        "BarDaxConn#close"
      );
    }
  }
}
