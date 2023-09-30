package com.github.sttk.sabi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Field;

import com.github.sttk.sabi.errs.Err;
import com.github.sttk.sabi.DaxBase.*;
import com.github.sttk.sabi.DaxBaseTest.*;
import static com.github.sttk.sabi.DaxBaseTest.*;

public class DaxBaseTest {

  record FailToSetupFooDaxSrc() {}
  record FailToSetupBarDaxSrc() {}
  record FailToCreateFooDaxConn() {}
  record FailToCreateBarDaxConn() {}
  record CreatedFooDaxConnIsNull() {}
  record CreatedBarDaxConnIsNull() {}
  record FailToCommitFooDaxConn() {}
  record FailToCommitBarDaxConn() {}
  record FailToCommitQuxDaxConn() {}

  static List<String> logs = new ArrayList<>();

  static boolean willFailToSetupFooDaxSrc = false;
  static boolean willFailToSetupBarDaxSrc = false;
  static boolean willFailToCreateFooDaxConn = false;
  static boolean willFailToCreateBarDaxConn = false;
  static boolean willCreatedFooDaxConnBeNull = false;
  static boolean willCreatedBarDaxConnBeNull = false;
  static boolean willFailToCommitFooDaxConn = false;
  static boolean willFailToCommitBarDaxConn = false;

  static boolean getIsGlobalDaxSrcsFixed() {
    final var c = DaxBase.class;
    try {
      final var f = c.getDeclaredField("isGlobalDaxSrcsFixed");
      f.setAccessible(true);
      return f.getBoolean(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static void setIsGlobalDaxSrcsFixed(boolean b) {
    final var c = DaxBase.class;
    try {
      final var f = c.getDeclaredField("isGlobalDaxSrcsFixed");
      f.setAccessible(true);
      f.setBoolean(null, b);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static Map<String, DaxSrc> getGlobalDaxSrcMap() {
    final var c = DaxBase.class;
    try {
      final Field f = c.getDeclaredField("globalDaxSrcMap");
      f.setAccessible(true);
      @SuppressWarnings("unchecked")
      final var m = (Map<String, DaxSrc>) f.get(null);
      return m;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static boolean getIsLocalDaxSrcsFixed(DaxBase base) {
    final var c = DaxBase.class;
    try {
      final var f = c.getDeclaredField("isLocalDaxSrcsFixed");
      f.setAccessible(true);
      return f.getBoolean(base);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static void setIsLocalDaxSrcsFixed(DaxBase base, boolean b) {
    final var c = DaxBase.class;
    try {
      final var f = c.getDeclaredField("isLocalDaxSrcsFixed");
      f.setAccessible(true);
      f.setBoolean(base, b);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static Map<String, DaxSrc> getLocalDaxSrcMap(DaxBase base) {
    final var c = DaxBase.class;
    try {
      final Field f = c.getDeclaredField("localDaxSrcMap");
      f.setAccessible(true);
      @SuppressWarnings("unchecked")
      final var m = (Map<String, DaxSrc>) f.get(base);
      return m;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static class FooDaxSrc implements DaxSrc {
    @Override
    public void setup(AsyncGroup ag) throws Err {
      if (willFailToSetupFooDaxSrc) {
        throw new Err(new FailToSetupFooDaxSrc());
      }
      logs.add("FooDaxSrc#setup");
    }
    @Override
    public void close() {
      logs.add("FooDaxSrc#close");
    }
    @Override
    public DaxConn createDaxConn() throws Err {
      if (willFailToCreateFooDaxConn) {
        throw new Err(new FailToCreateFooDaxConn());
      }
      if (willCreatedFooDaxConnBeNull) {
        return null;
      }
      logs.add("FooDaxSrc#createDaxConn");
      return new FooDaxConn();
    }
  }

  static class FooDaxConn implements DaxConn {
    boolean committed = false;
    @Override
    public void commit(AsyncGroup ag) throws Err {
      if (willFailToCommitFooDaxConn) {
        throw new Err(new FailToCommitFooDaxConn());
      }
      logs.add("FooDaxConn#commit");
      committed = true;
    }
    @Override
    public boolean isCommitted() {
      return committed;
    }
    @Override
    public void rollback(AsyncGroup ag) {
      logs.add("FooDaxConn#rollback");
    }
    @Override
    public void forceBack(AsyncGroup ag) {
      logs.add("FooDaxConn#forceBack");
    }
    @Override
    public void close() {
      logs.add("FooDaxConn#close");
    }
  }

  static class BarDaxSrc implements DaxSrc {
    @Override
    public void setup(AsyncGroup ag) throws Err {
      ag.add(() -> {
        if (willFailToSetupBarDaxSrc) {
          throw new Err(new FailToSetupBarDaxSrc());
        }
        logs.add("BarDaxSrc#setup");
      });
    }
    @Override
    public void close() {
      logs.add("BarDaxSrc#close");
    }
    @Override
    public DaxConn createDaxConn() throws Err {
      if (willFailToCreateBarDaxConn) {
        throw new Err(new FailToCreateBarDaxConn());
      }
      if (willCreatedBarDaxConnBeNull) {
        return null;
      }
      logs.add("BarDaxSrc#createDaxConn");
      return new BarDaxConn();
    }
  }

  static class BarDaxConn implements DaxConn {
    boolean committed = false;
    @Override
    public void commit(AsyncGroup ag) throws Err {
      ag.add(() -> {
        if (willFailToCommitBarDaxConn) {
          throw new Err(new FailToCommitBarDaxConn());
        }
        logs.add("BarDaxConn#commit");
        committed = true;
      });
    }
    @Override
    public boolean isCommitted() {
      return committed;
    }
    @Override
    public void rollback(AsyncGroup ag) {
      ag.add(() -> {
        logs.add("BarDaxConn#rollback");
      });
    }
    @Override
    public void forceBack(AsyncGroup ag) {
      ag.add(() -> {
        logs.add("BarDaxConn#forceBack");
      });
    }
    @Override
    public void close() {
      logs.add("BarDaxConn#close");
    }
  }

  static class QuxDaxSrc implements DaxSrc {
    @Override
    public void setup(AsyncGroup ag) throws Err {
      logs.add("QuxDaxSrc#setup");
    }
    @Override
    public void close() {
      logs.add("QuxDaxSrc#close");
    }
    @Override
    public DaxConn createDaxConn() throws Err {
      logs.add("QuxDaxSrc#createDaxConn");
      return new QuxDaxConn();
    }
  }

  static class QuxDaxConn implements DaxConn {
    @Override
    public void commit(AsyncGroup ag) throws Err {
      throw new Err(new FailToCommitQuxDaxConn());
    }
    @Override
    public void forceBack(AsyncGroup ag) {
      logs.add("QuxDaxConn#forceBack");
    }
    @Override
    public void close() {
      logs.add("QuxDaxConn#close");
    }
  }

  ///

  @BeforeEach
  void reset() {
    setIsGlobalDaxSrcsFixed(false);
    getGlobalDaxSrcMap().clear();

    willFailToSetupFooDaxSrc = false;
    willFailToSetupBarDaxSrc = false;
    willFailToCreateFooDaxConn = false;
    willFailToCreateBarDaxConn = false;
    willCreatedFooDaxConnBeNull = false;
    willCreatedBarDaxConnBeNull = false;
    willFailToCommitFooDaxConn = false;
    willFailToCommitBarDaxConn = false;

    logs.clear();
  }

  ///

  @Test
  public void new_and_close() {
    assertThat(getIsGlobalDaxSrcsFixed()).isFalse();

    var base = new DaxBase();
    assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
    assertThat(getIsLocalDaxSrcsFixed(base)).isFalse();
    assertThat(getLocalDaxSrcMap(base)).hasSize(0);

    base.close();
    assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
    assertThat(getIsLocalDaxSrcsFixed(base)).isFalse();
    assertThat(getLocalDaxSrcMap(base)).hasSize(0);

    assertThat(logs).hasSize(0);
  }

  @Test
  public void uses_ok() {
    assertThat(getIsGlobalDaxSrcsFixed()).isFalse();
    assertThat(getGlobalDaxSrcMap()).hasSize(0);

    try (var base = new DaxBase()) {
      assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
      assertThat(getIsLocalDaxSrcsFixed(base)).isFalse();
      assertThat(getLocalDaxSrcMap(base)).hasSize(0);

      base.uses("httpRequest", new FooDaxSrc());

      assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
      assertThat(getIsLocalDaxSrcsFixed(base)).isFalse();
      var m = getLocalDaxSrcMap(base);
      assertThat(m).hasSize(1);
      assertThat(m.get("httpRequest")).isInstanceOf(FooDaxSrc.class);
    } catch (Err e) {
      fail(e);
    }

    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void uses_failToSetupDaxSrc_sync() {
    assertThat(getIsGlobalDaxSrcsFixed()).isFalse();
    assertThat(getGlobalDaxSrcMap()).hasSize(0);

    willFailToSetupFooDaxSrc = true;

    try (var base = new DaxBase()) {
      base.uses("httpRequest", new FooDaxSrc());
      fail();
    } catch (Err e) {
      switch (e.getReason()) {
        case FailToSetupLocalDaxSrc r: {
          assertThat(r.name()).isEqualTo("httpRequest");
          var e1 = Err.class.cast(e.getCause());
          assertThat(e1.getReason()).isInstanceOf(FailToSetupFooDaxSrc.class);
          break;
        }
        default: {
          fail();
          break;
        }
      }
    }

    assertThat(logs).hasSize(0);
  }

  @Test
  public void uses_failToSetupDaxSrc_async() {
    assertThat(getIsGlobalDaxSrcsFixed()).isFalse();
    assertThat(getGlobalDaxSrcMap()).hasSize(0);

    willFailToSetupBarDaxSrc = true;

    try (var base = new DaxBase()) {
      base.uses("httpRequest", new BarDaxSrc());
      fail();
    } catch (Err e) {
      switch (e.getReason()) {
        case FailToSetupLocalDaxSrc r: {
          assertThat(r.name()).isEqualTo("httpRequest");
          var e1 = Err.class.cast(e.getCause());
          assertThat(e1.getReason()).isInstanceOf(FailToSetupBarDaxSrc.class);
          break;
        }
        default: {
          fail();
          break;
        }
      }
    }

    assertThat(logs).hasSize(0);
  }

  @Test
  public void uses_txnAlreadyBegan() {
    try (var base = new DaxBase()) {
      base.begin();
      base.uses("httpRequest", new BarDaxSrc());
      base.end();
    } catch (Err e) {
      fail(e);
    }

    assertThat(logs).hasSize(0);
  }

  @Test
  public void close_txnAlreadyBegan() {
    try {
      var base = new DaxBase();
      base.uses("httpRequest", new FooDaxSrc());
      base.begin();
      base.close();
      base.end();
    } catch (Err e) {
      fail(e);
    }

    assertThat(logs).hasSize(1);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
  }

  @Test
  public void disuses_ok() {
    try {
      var base = new DaxBase();
      base.uses("httpRequest", new FooDaxSrc());
      base.disuses("httpRequest");
    } catch (Err e) {
      fail(e);
    }

    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void disuses_ignoreIfNoDaxSrc() {
    try {
      var base = new DaxBase();
      base.uses("httpRequest", new FooDaxSrc());
      base.disuses("xxx");
    } catch (Err e) {
      fail(e);
    }

    assertThat(logs).hasSize(1);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
  }

  @Test
  public void disuses_txnAlreadyBegan() {
    try {
      var base = new DaxBase();
      base.uses("httpRequest", new FooDaxSrc());
      base.begin();
      base.disuses("httpRequest");
      base.end();
    } catch (Err e) {
      fail(e);
    }

    assertThat(logs).hasSize(1);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
  }

  @Test
  public void txn_ok() {
    try (var base = new DaxBase()) {
      base.txn((Dax dax) -> {
        logs.add("exec logic");
      });
    } catch (Err e) {
      fail(e);
    }

    assertThat(logs).hasSize(1);
    assertThat(logs.get(0)).isEqualTo("exec logic");
  }

  @Test
  public void txn_withCustomDax() {
    interface MyLogicDax {
      String getData() throws Err;
      void setData(String v) throws Err;
    }

    interface AllLogicDax extends MyLogicDax {}

    interface MyDax extends Dax, AllLogicDax {
      default String getData() throws Err {
        var conn = getDaxConn("mydata");
        return "hello";
      }
      default void setData(String v) throws Err {
        var conn = getDaxConn("mydata");
      }
    }

    class MyDaxBase extends DaxBase implements MyDax {}

    try (var base = new MyDaxBase()) {
      base.uses("mydata", new FooDaxSrc());
      base.txn((MyLogicDax dax) -> {
        var s = dax.getData();
        dax.setData(s);
      });
    } catch (Err e) {
      fail(e);
    }

    assertThat(logs).hasSize(5);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("FooDaxSrc#createDaxConn");
    assertThat(logs.get(2)).isEqualTo("FooDaxConn#commit");
    assertThat(logs.get(3)).isEqualTo("FooDaxConn#close");
    assertThat(logs.get(4)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void txn_failToCastDaxBase() {
    interface MyLogicDax {
      String getData() throws Err;
      void setData(String v) throws Err;
    }

    class MyLogic implements Logic<MyLogicDax> {
      @Override public void run(MyLogicDax dax) throws Err {
        var s = dax.getData();
        dax.setData(s);
      }
    }

    interface AllLogicDax {}

    interface MyDax extends Dax, AllLogicDax {
      default String getData() throws Err {
        var conn = getDaxConn("mydata");
        return "hello";
      }
      default void setData(String v) throws Err {
        var conn = getDaxConn("mydata");
      }
    }

    class MyDaxBase extends DaxBase implements MyDax {}

    try (var base = new MyDaxBase()) {
      base.uses("mydata", new FooDaxSrc());
      base.txn(new MyLogic());
      fail();
    } catch (Err e) {
      switch (e.getReason()) {
        case FailToRunLogic r: {
          assertThat(r.logicType()).isEqualTo(MyLogic.class);
          assertThat(e.getCause()).isInstanceOf(ClassCastException.class);
          break;
        }
        default: {
          fail(e);
        }
      }
    }

    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void txn_failToCastDaxConn() {
    interface MyLogicDax {
      String getData() throws Err;
      void setData(String v) throws Err;
    }

    class MyLogic implements Logic<MyLogicDax> {
      @Override public void run(MyLogicDax dax) throws Err {
        var s = dax.getData();
        dax.setData(s);
      }
    }

    interface AllLogicDax extends MyLogicDax {}

    interface MyDax extends Dax, AllLogicDax {
      default String getData() throws Err {
        var conn = getDaxConn("mydata");
        return "hello";
      }
      default void setData(String v) throws Err {
        BarDaxConn conn = getDaxConn("mydata");
      }
    }

    class MyDaxBase extends DaxBase implements MyDax {}

    try (var base = new MyDaxBase()) {
      base.uses("mydata", new FooDaxSrc());
      base.txn(new MyLogic());
      fail();
    } catch (Err e) {
      switch (e.getReason()) {
        case FailToRunLogic r: {
          assertThat(r.logicType()).isEqualTo(MyLogic.class);
          assertThat(e.getCause()).isInstanceOf(ClassCastException.class);
          break;
        }
        default: {
          fail(e);
        }
      }
    }

    assertThat(logs).hasSize(5);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("FooDaxSrc#createDaxConn");
    assertThat(logs.get(2)).isEqualTo("FooDaxConn#rollback");
    assertThat(logs.get(3)).isEqualTo("FooDaxConn#close");
    assertThat(logs.get(4)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void txn_errorFromLogic() {
    interface MyLogicDax {
      String getData() throws Err;
      void setData(String v) throws Err;
    }

    interface AllLogicDax extends MyLogicDax {}

    interface MyDax extends Dax, AllLogicDax {
      default String getData() throws Err {
        var conn = getDaxConn("mydata");
        return "hello";
      }
      default void setData(String v) throws Err {
        BarDaxConn conn = getDaxConn("mydata");
      }
    }

    class MyDaxBase extends DaxBase implements MyDax {}

    record FailToDoSomething() {}

    try (var base = new MyDaxBase()) {
      base.uses("mydata", new FooDaxSrc());
      base.txn((MyLogicDax dax) -> {
        throw new Err(new FailToDoSomething());
      });
      fail();
    } catch (Err e) {
      switch (e.getReason()) {
        case FailToDoSomething r: {
          break;
        }
        default: {
          fail(e);
        }
      }
    }

    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void txn_failToCreateDaxConn() {
    interface MyLogicDax {
      String getData() throws Err;
      void setData(String v) throws Err;
    }

    class MyLogic implements Logic<MyLogicDax> {
      @Override public void run(MyLogicDax dax) throws Err {
        var s = dax.getData();
        dax.setData(s);
      }
    }

    interface AllLogicDax extends MyLogicDax {}

    interface MyDax extends Dax, AllLogicDax {
      default String getData() throws Err {
        var conn = getDaxConn("mydata");
        return "hello";
      }
      default void setData(String v) throws Err {
        var conn = getDaxConn("mydata");
      }
    }

    class MyDaxBase extends DaxBase implements MyDax {}

    willFailToCreateFooDaxConn = true;

    try (var base = new MyDaxBase()) {
      base.uses("mydata", new FooDaxSrc());
      base.txn(new MyLogic());
      fail();
    } catch (Err e) {
      switch (e.getReason()) {
        case FailToCreateDaxConn r: {
          assertThat(r.name()).isEqualTo("mydata");
          var r2 = Err.class.cast(e.getCause()).getReason();
          assertThat(r2).isInstanceOf(FailToCreateFooDaxConn.class);
          break;
        }
        default: {
          fail(e);
        }
      }
    }

    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void txn_errorBecauseCreatedDaxConnIsNull() {
    interface MyLogicDax {
      String getData() throws Err;
      void setData(String v) throws Err;
    }

    class MyLogic implements Logic<MyLogicDax> {
      @Override public void run(MyLogicDax dax) throws Err {
        var s = dax.getData();
        dax.setData(s);
      }
    }

    interface AllLogicDax extends MyLogicDax {}

    interface MyDax extends Dax, AllLogicDax {
      default String getData() throws Err {
        var conn = getDaxConn("mydata");
        return "hello";
      }
      default void setData(String v) throws Err {
        var conn = getDaxConn("mydata");
      }
    }

    class MyDaxBase extends DaxBase implements MyDax {}

    willCreatedFooDaxConnBeNull = true;

    try (var base = new MyDaxBase()) {
      base.uses("mydata", new FooDaxSrc());
      base.txn(new MyLogic());
      fail();
    } catch (Err e) {
      switch (e.getReason()) {
        case CreatedDaxConnIsNull r: {
          assertThat(r.name()).isEqualTo("mydata");
          break;
        }
        default: {
          fail(e);
        }
      }
    }

    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void txn_failBecauseDaxSrcIsNotFound() {
    interface MyLogicDax {
      String getData() throws Err;
      void setData(String v) throws Err;
    }

    interface AllLogicDax extends MyLogicDax {}

    interface MyDax extends Dax, AllLogicDax {
      default String getData() throws Err {
        var conn = getDaxConn("mydata");
        return "hello";
      }
      default void setData(String v) throws Err {
        var conn = getDaxConn("mydata");
      }
    }

    class MyDaxBase extends DaxBase implements MyDax {}

    try (var base = new MyDaxBase()) {
      base.txn((MyLogicDax dax) -> {
        var s = dax.getData();
        dax.setData(s);
      });
      fail();
    } catch (Err e) {
      switch (e.getReason()) {
        case DaxSrcIsNotFound r: {
          assertThat(r.name()).isEqualTo("mydata");
          break;
        }
        default: {
          fail(e);
        }
      }
    }

    assertThat(logs).hasSize(0);
  }

  @Test
  public void txn_failToCommitDaxConn_sync() {
    interface MyLogicDax {
      String getData() throws Err;
      void setData(String v) throws Err;
    }

    interface AllLogicDax extends MyLogicDax {}

    interface MyDax extends Dax, AllLogicDax {
      default String getData() throws Err {
        var conn = getDaxConn("mydata");
        return "hello";
      }
      default void setData(String v) throws Err {
        var conn = getDaxConn("mydata");
      }
    }

    class MyDaxBase extends DaxBase implements MyDax {}

    willFailToCommitFooDaxConn = true;

    try (var base = new MyDaxBase()) {
      base.uses("mydata", new FooDaxSrc());
      base.txn((MyLogicDax dax) -> {
        var s = dax.getData();
        dax.setData(s);
      });
      fail();
    } catch (Err e) {
      switch (e.getReason()) {
        case FailToCommitDaxConn r: {
          var m = r.errors();
          assertThat(m).hasSize(1);
          var r1 = m.get("mydata").getReason();
          assertThat(r1).isInstanceOf(FailToCommitFooDaxConn.class);
          break;
        }
        default: {
          fail(e);
        }
      }
    }

    assertThat(logs).hasSize(5);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("FooDaxSrc#createDaxConn");
    assertThat(logs.get(2)).isEqualTo("FooDaxConn#rollback");
    assertThat(logs.get(3)).isEqualTo("FooDaxConn#close");
    assertThat(logs.get(4)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void txn_failToCommitDaxConn_async() {
    interface MyLogicDax {
      String getData() throws Err;
      void setData(String v) throws Err;
    }

    interface AllLogicDax extends MyLogicDax {}

    interface MyDax extends Dax, AllLogicDax {
      default String getData() throws Err {
        var conn = getDaxConn("mydata");
        return "hello";
      }
      default void setData(String v) throws Err {
        var conn = getDaxConn("mydata");
      }
    }

    class MyDaxBase extends DaxBase implements MyDax {}

    willFailToCommitBarDaxConn = true;

    try (var base = new MyDaxBase()) {
      base.uses("mydata", new BarDaxSrc());
      base.txn((MyLogicDax dax) -> {
        var s = dax.getData();
        dax.setData(s);
      });
      fail();
    } catch (Err e) {
      switch (e.getReason()) {
        case FailToCommitDaxConn r: {
          var m = r.errors();
          assertThat(m).hasSize(1);
          var r1 = m.get("mydata").getReason();
          assertThat(r1).isInstanceOf(FailToCommitBarDaxConn.class);
          break;
        }
        default: {
          fail(e);
        }
      }
    }

    assertThat(logs).hasSize(5);
    assertThat(logs.get(0)).isEqualTo("BarDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("BarDaxSrc#createDaxConn");
    assertThat(logs.get(2)).isEqualTo("BarDaxConn#rollback");
    assertThat(logs.get(3)).isEqualTo("BarDaxConn#close");
    assertThat(logs.get(4)).isEqualTo("BarDaxSrc#close");
  }

  @Test
  public void txn_forceBack() {
    interface MyLogicDax {
      String getData() throws Err;
      void setData(String v) throws Err;
    }

    interface AllLogicDax extends MyLogicDax {}

    interface MyDax extends Dax, AllLogicDax {
      default String getData() throws Err {
        var conn = getDaxConn("foo");
        return "hello";
      }
      default void setData(String v) throws Err {
        var conn = getDaxConn("bar");
      }
    }

    class MyDaxBase extends DaxBase implements MyDax {}

    willFailToCommitBarDaxConn = true;

    try (var base = new MyDaxBase()) {
      base.uses("foo", new FooDaxSrc());
      base.uses("bar", new BarDaxSrc());
      base.txn((MyLogicDax dax) -> {
        var s = dax.getData();
        dax.setData(s);
      });
      fail();
    } catch (Err e) {
      switch (e.getReason()) {
        case FailToCommitDaxConn r: {
          var m = r.errors();
          assertThat(m).hasSize(1);
          var r1 = m.get("bar").getReason();
          assertThat(r1).isInstanceOf(FailToCommitBarDaxConn.class);
          break;
        }
        default: {
          fail(e);
        }
      }
    }

    assertThat(logs).hasSize(11);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("BarDaxSrc#setup");
    assertThat(logs.get(2)).isEqualTo("FooDaxSrc#createDaxConn");
    assertThat(logs.get(3)).isEqualTo("BarDaxSrc#createDaxConn");
    assertThat(logs.get(4)).isEqualTo("FooDaxConn#commit");
    assertThat(logs.get(5)).isEqualTo("FooDaxConn#forceBack");
    assertThat(logs.get(6)).isEqualTo("BarDaxConn#rollback");
    assertThat(logs.get(7)).isEqualTo("FooDaxConn#close");
    assertThat(logs.get(8)).isEqualTo("BarDaxConn#close");
    assertThat(logs.get(9)).isEqualTo("FooDaxSrc#close");
    assertThat(logs.get(10)).isEqualTo("BarDaxSrc#close");
  }

  @Test
  public void txn_whenDaxConnHasNoRollbackMechanism() {
    interface MyLogicDax {
      String getData() throws Err;
      void setData(String v) throws Err;
    }

    interface AllLogicDax extends MyLogicDax {}

    interface MyDax extends Dax, AllLogicDax {
      default String getData() throws Err {
        var conn = getDaxConn("mydata");
        return "hello";
      }
      default void setData(String v) throws Err {
        var conn = getDaxConn("mydata");
      }
    }

    class MyDaxBase extends DaxBase implements MyDax {}

    try (var base = new MyDaxBase()) {
      base.uses("mydata", new QuxDaxSrc());
      base.txn((MyLogicDax dax) -> {
        var s = dax.getData();
        dax.setData(s);
      });
      fail();
    } catch (Err e) {
      switch (e.getReason()) {
        case FailToCommitDaxConn r: {
          var m = r.errors();
          assertThat(m).hasSize(1);
          var r1 = m.get("mydata").getReason();
          assertThat(r1).isInstanceOf(FailToCommitQuxDaxConn.class);
          break;
        }
        default: {
          fail(e);
        }
      }
    }

    assertThat(logs).hasSize(5);
    assertThat(logs.get(0)).isEqualTo("QuxDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("QuxDaxSrc#createDaxConn");
    assertThat(logs.get(2)).isEqualTo("QuxDaxConn#forceBack");
    assertThat(logs.get(3)).isEqualTo("QuxDaxConn#close");
    assertThat(logs.get(4)).isEqualTo("QuxDaxSrc#close");
  }
}
