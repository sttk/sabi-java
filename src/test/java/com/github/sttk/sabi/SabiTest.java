package com.github.sttk.sabi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.github.sttk.sabi.errs.Err;
import com.github.sttk.sabi.DaxBase.*;
import com.github.sttk.sabi.DaxBaseTest.*;
import static com.github.sttk.sabi.DaxBaseTest.*;

public class SabiTest {

  @BeforeEach
  void reset() {
    new DaxBaseTest().reset();
  }

  ///

  @Test
  public void should_register_global_DaxSrc() {
    Sabi.uses("cliargs", new FooDaxSrc());

    final Map<String, DaxSrc> m = getGlobalDaxSrcMap();
    assertThat(m).hasSize(1);
    assertThat(m.get("cliargs")).isInstanceOf(FooDaxSrc.class);

    Sabi.uses("database", new BarDaxSrc());

    assertThat(m).hasSize(2);
    assertThat(m.get("cliargs")).isInstanceOf(FooDaxSrc.class);
    assertThat(m.get("database")).isInstanceOf(BarDaxSrc.class);
  }

  @Test
  public void should_register_global_DaxSrc_but_name_already_exists() {
    Sabi.uses("cliargs", new FooDaxSrc());

    final Map<String, DaxSrc> m = getGlobalDaxSrcMap();
    assertThat(m).hasSize(1);
    assertThat(m.get("cliargs")).isInstanceOf(FooDaxSrc.class);

    Sabi.uses("cliargs", new BarDaxSrc());

    assertThat(m).hasSize(1);
    assertThat(m.get("cliargs")).isInstanceOf(FooDaxSrc.class);
  }

  @Test
  public void should_setup_zero_global_DaxSrc() {
    final Map<String, DaxSrc> m = getGlobalDaxSrcMap();

    assertThat(getIsGlobalDaxSrcsFixed()).isFalse();
    assertThat(m).hasSize(0);
    assertThat(logs).hasSize(0);

    try {
      Sabi.setup();
    } catch (Exception e) {
      fail(e);
    } finally {
      Sabi.close();
    }

    assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
    assertThat(m).hasSize(0);
    assertThat(logs).hasSize(0);
  }

  @Test
  public void should_setup_one_global_DaxSrc() {
    final Map<String, DaxSrc> m = getGlobalDaxSrcMap();

    Sabi.uses("cliargs", new FooDaxSrc());

    assertThat(getIsGlobalDaxSrcsFixed()).isFalse();
    assertThat(m).hasSize(1);
    assertThat(m.get("cliargs")).isInstanceOf(FooDaxSrc.class);
    assertThat(logs).hasSize(0);

    try {
      Sabi.setup();
    } catch (Exception e) {
      fail(e);
    } finally {
      Sabi.close();
    }

    assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
    assertThat(m).hasSize(1);
    assertThat(m.get("cliargs")).isInstanceOf(FooDaxSrc.class);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void should_setup_multiple_global_DaxSrc() {
    final Map<String, DaxSrc> m = getGlobalDaxSrcMap();

    Sabi.uses("cliargs", new FooDaxSrc());
    Sabi.uses("database", new BarDaxSrc());

    assertThat(getIsGlobalDaxSrcsFixed()).isFalse();
    assertThat(m).hasSize(2);
    assertThat(m.get("cliargs")).isInstanceOf(FooDaxSrc.class);
    assertThat(m.get("database")).isInstanceOf(BarDaxSrc.class);
    assertThat(logs).hasSize(0);

    try {
      Sabi.setup();
    } catch (Exception e) {
      fail(e);
    } finally {
      Sabi.close();
    }

    assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
    assertThat(m).hasSize(2);
    assertThat(m.get("cliargs")).isInstanceOf(FooDaxSrc.class);
    assertThat(m.get("database")).isInstanceOf(BarDaxSrc.class);
    assertThat(logs).hasSize(4);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("BarDaxSrc#setup");
    assertThat(logs.get(2)).isEqualTo("FooDaxSrc#close");
    assertThat(logs.get(3)).isEqualTo("BarDaxSrc#close");
  }

  @Test
  public void should_setup_and_cannot_add_after_setup() {
    final Map<String, DaxSrc> m = getGlobalDaxSrcMap();

    Sabi.uses("cliargs", new FooDaxSrc());

    assertThat(getIsGlobalDaxSrcsFixed()).isFalse();
    assertThat(m).hasSize(1);
    assertThat(m.get("cliargs")).isInstanceOf(FooDaxSrc.class);
    assertThat(logs).hasSize(0);

    try {
      Sabi.setup();
    } catch (Exception e) {
      fail(e);
    } finally {
      Sabi.close();
    }

    assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
    assertThat(m).hasSize(1);
    assertThat(m.get("cliargs")).isInstanceOf(FooDaxSrc.class);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("FooDaxSrc#close");

    Sabi.uses("database", new FooDaxSrc());

    assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
    assertThat(m).hasSize(1);
    assertThat(m.get("cliargs")).isInstanceOf(FooDaxSrc.class);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void should_setup_sync_but_error() {
    final Map<String, DaxSrc> m = getGlobalDaxSrcMap();

    willFailToSetupFooDaxSrc = true;

    Sabi.uses("cliargs", new FooDaxSrc());

    try {
      Sabi.setup();
      fail();
    } catch (Err e) {
      assertThat(e.getReason()).isInstanceOf(FailToSetupGlobalDaxSrcs.class);
      switch (e.getReason()) {
        case FailToSetupGlobalDaxSrcs r: {
          assertThat(r.errors()).hasSize(1);
          var e1 = r.errors().get("cliargs");
          assertThat(e1.getReason()).isInstanceOf(FailToSetupFooDaxSrc.class);
          break;
        }
        default: {
          fail();
          break;
        }
      }
    } catch (Exception e) {
      fail(e);
    } finally {
      Sabi.close();
    }

    assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
    assertThat(m).hasSize(1);
    assertThat(m.get("cliargs")).isInstanceOf(FooDaxSrc.class);
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void should_setup_async_but_error() {
    final Map<String, DaxSrc> m = getGlobalDaxSrcMap();

    willFailToSetupBarDaxSrc = true;

    Sabi.uses("cliargs", new BarDaxSrc());

    try {
      Sabi.setup();
      fail();
    } catch (Err e) {
      assertThat(e.getReason()).isInstanceOf(FailToSetupGlobalDaxSrcs.class);
      switch (e.getReason()) {
        case FailToSetupGlobalDaxSrcs r: {
          assertThat(r.errors()).hasSize(1);
          var e1 = r.errors().get("cliargs");
          assertThat(e1.getReason()).isInstanceOf(FailToSetupBarDaxSrc.class);
          break;
        }
        default: {
          fail();
          break;
        }
      }
    } catch (Exception e) {
      fail(e);
    } finally {
      Sabi.close();
    }

    assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
    assertThat(m).hasSize(1);
    assertThat(m.get("cliargs")).isInstanceOf(BarDaxSrc.class);
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0)).isEqualTo("BarDaxSrc#close");
  }

  @Test
  public void should_setup_sync_and_async_but_error() {
    final Map<String, DaxSrc> m = getGlobalDaxSrcMap();

    willFailToSetupFooDaxSrc = true;
    willFailToSetupBarDaxSrc = true;

    Sabi.uses("cliargs", new BarDaxSrc());
    Sabi.uses("database", new FooDaxSrc());

    try {
      Sabi.setup();
      fail();
    } catch (Err e) {
      assertThat(e.getReason()).isInstanceOf(FailToSetupGlobalDaxSrcs.class);
      switch (e.getReason()) {
        case FailToSetupGlobalDaxSrcs r: {
          assertThat(r.errors()).hasSize(2);
          var e1 = r.errors().get("cliargs");
          assertThat(e1.getReason()).isInstanceOf(FailToSetupBarDaxSrc.class);
          var e2 = r.errors().get("database");
          assertThat(e2.getReason()).isInstanceOf(FailToSetupFooDaxSrc.class);
          break;
        }
        default: {
          fail();
          break;
        }
      }
    } catch (Exception e) {
      fail(e);
    } finally {
      Sabi.close();
    }

    assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
    assertThat(m).hasSize(2);
    assertThat(m.get("cliargs")).isInstanceOf(BarDaxSrc.class);
    assertThat(m.get("database")).isInstanceOf(FooDaxSrc.class);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("BarDaxSrc#close");
    assertThat(logs.get(1)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void should_do_startApp_and_ok() {
    final Map<String, DaxSrc> m = getGlobalDaxSrcMap();

    Sabi.uses("database", new FooDaxSrc());

    final Runner app = () -> {
      logs.add("run app");
    };

    try (var ac = Sabi.startApp()) {
      app.run();
    } catch (Exception e) {
      fail(e);
    }

    assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
    assertThat(m).hasSize(1);
    assertThat(m.get("database")).isInstanceOf(FooDaxSrc.class);
    assertThat(logs).hasSize(3);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("run app");
    assertThat(logs.get(2)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void should_do_startApp_but_fail_to_setup() {
    final Map<String, DaxSrc> m = getGlobalDaxSrcMap();

    Sabi.uses("database", new FooDaxSrc());

    willFailToSetupFooDaxSrc = true;

    final Runner app = () -> {
      logs.add("run app");
    };

    try (var c = Sabi.startApp()) {
      app.run();
      fail();
    } catch (Err e) {
      switch (e.getReason()) {
        case FailToSetupGlobalDaxSrcs r: {
          assertThat(r.errors()).hasSize(1);
          var r2 = r.errors().get("database").getReason();
          assertThat(r2).isInstanceOf(FailToSetupFooDaxSrc.class);
          break;
        }
        default: {
          fail(e);
          break;
        }
      }
    } catch (Exception e) {
      fail(e);
    }

    assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
    assertThat(m).hasSize(1);
    assertThat(m.get("database")).isInstanceOf(FooDaxSrc.class);
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#close");
  }

  @Test
  public void should_do_startApp_but_app_failed() {
    final Map<String, DaxSrc> m = getGlobalDaxSrcMap();

    Sabi.uses("database", new FooDaxSrc());

    record FailToDoSomething() {}

    final Runner app = () -> {
      throw new Err(new FailToDoSomething());
    };

    try (var c = Sabi.startApp()) {
      app.run();
    } catch (Err e) {
      switch (e.getReason()) {
        case FailToDoSomething r: {
          break;
        }
        default: {
          fail(e);
          break;
        }
      }
    } catch (Exception e) {
      fail(e);
    }

    assertThat(getIsGlobalDaxSrcsFixed()).isTrue();
    assertThat(m).hasSize(1);
    assertThat(m.get("database")).isInstanceOf(FooDaxSrc.class);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("FooDaxSrc#setup");
    assertThat(logs.get(1)).isEqualTo("FooDaxSrc#close");
  }
}
