import com.github.sttk.sabi.*;
import com.github.sttk.sabi.errs.*;

import java.util.Map;
import java.util.HashMap;
import java.time.OffsetTime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ReadmeTest {

  @Test void testGreetLogic_morning() {
    var base = new MapGreetDaxBase();
    base.m.put("username", "everyone");
    base.m.put("hour", 10);

    try (base) {
      base.txn(new GreetLogic());
    } catch (Err e) {
      fail(e.toString());
    }

    assertEquals(base.m.get("greeting"), "Good morning, everyone.\n");
  }

  @Test void testGreetApp_main() {
    setenv("GREETING_USERNAME", "foo");
    setenv("GREETING_HOUR", "10");

    try (var ac = Sabi.startApp()) {
      executeApp();
    } catch (Exception e) {
      //System.err.println(e.toString());
      //System.exit(1);
      fail(e.toString());
    }
  }
  void executeApp() throws Err {
    try (var base = new GreetDaxBase()) {
      base.uses("memory", new MemoryDaxSrc());
      base.txn(new GreetLogic());
    }
  }

  static void setenv(String name, String value) {
    try {
      String os = System.getProperty("os.name").toLowerCase();

      if (os.startsWith("win")) {
        var cls = Class.forName("java.lang.ProcessEnvironment");
        var fld = cls.getDeclaredField("theCaseInsensitiveEnvironment");
        fld.setAccessible(true);
        @SuppressWarnings("unchecked")
        var map = (Map<Object, Object>) fld.get(null);

        if (value == null) {
          map.remove(name);
        } else {
          map.put(name, value);
        }

      } else {
        var cls = Class.forName("java.lang.ProcessEnvironment$Variable");
        var mtd = cls.getDeclaredMethod("valueOf", String.class);
        mtd.setAccessible(true);
        var envName = mtd.invoke(null, name);

        cls = Class.forName("java.lang.ProcessEnvironment$Value");
        mtd = cls.getDeclaredMethod("valueOf", String.class);
        mtd.setAccessible(true);
        var envValue = mtd.invoke(null, value);

        cls = Class.forName("java.lang.ProcessEnvironment");
        var fld = cls.getDeclaredField("theEnvironment");
        fld.setAccessible(true);
        @SuppressWarnings("unchecked")
        var map = (Map<Object, Object>) fld.get(null);

        if (value == null) {
          map.remove(envName);
        } else {
          map.put(envName, envValue);
        }
      }
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Exception e) {
      var re = new RuntimeException(e);
      re.setStackTrace(e.getStackTrace());
      throw re;
    }
  }
}

interface GreetDax {
  record NoName() {}
  record FailToGetHour() {}
  record FailToOutput(String text) {}

  String getUserName() throws Err;
  int getHour() throws Err;
  void output(String text) throws Err;
}

class GreetLogic implements Logic<GreetDax> {
  @Override public void run(GreetDax dax) throws Err {
    int hour = dax.getHour();

    String s;
    if (5 <= hour && hour < 12) {
      s = "Good morning, ";
    } else if (12 <= hour && hour < 16) {
      s = "Good afternoon, ";
    } else if (16 <= hour && hour < 21) {
      s = "Good evening, ";
    } else {
      s = "Hi, ";
    }
    dax.output(s);

    var name = dax.getUserName();
    dax.output(name + ".\n");
  }
}

class MapGreetDaxBase extends DaxBase implements GreetDax {
  Map<String, Object> m = new HashMap<>();

  @Override public String getUserName() throws Err {
    var name = this.m.get("username");
    if (name == null) {
      throw new Err(new NoName());
    }
    return String.class.cast(name);
  }

  @Override public int getHour() throws Err {
    var hour = this.m.get("hour");
    if (hour == null) {
      throw new Err(new FailToGetHour());
    }
    return Integer.class.cast(hour);
  }

  @Override public void output(String text) throws Err {
    String s = "";
    var v = this.m.get("greeting");
    if ("error".equals(v)) {
      throw new Err(new FailToOutput(text));
    } else if (v != null) {
      s += v;
    }
    this.m.put("greeting", s + text);
  }
}

interface EnvVarsDax extends GreetDax, Dax {
  @Override default String getUserName() throws Err {
    var u = System.getenv("GREETING_USERNAME");
    if (u == null || u.isBlank()) {
      throw new Err(new NoName());
    }
    return u;
  }

  //@Override default int getHour() throws Err {
  //  var h = System.getenv("GREETING_HOUR");
  //  try {
  //    return Integer.valueOf(h);
  //  } catch (Exception e) {
  //    throw new Err(new FailToGetHour(), e);
  //  }
  //}
}

//interface ConsoleDax extends GreetDax, Dax {
//  @Override default void output(String text) throws Err {
interface ConsoleDax extends PrintDax, Dax {
  @Override default void print(String text) throws Err {
    System.out.print(text);
  }
}

class GreetDaxBase extends DaxBase
  implements EnvVarsDax, SystemClockDax, MemoryDax, ConsoleDax {}

interface SystemClockDax extends GreetDax, Dax {
  @Override default int getHour() throws Err {
    return OffsetTime.now().getHour();
  }
}

interface PrintDax {
  String getText() throws Err;
  void print(String text) throws Err;
}

class PrintLogic implements Logic<PrintDax> {
  @Override public void  run(PrintDax dax) throws Err {
    var text = dax.getText();
    dax.print(text);
  }
}

class MemoryDaxSrc implements DaxSrc {
  StringBuilder buf = new StringBuilder();

  @Override public void setup(AsyncGroup ag) throws Err {
  }

  @Override public void close() {
    buf.setLength(0);
  }

  @Override public DaxConn createDaxConn() throws Err {
    return new MemoryDaxConn(buf);
  }
}

class MemoryDaxConn implements DaxConn {
  StringBuilder buf;

  public MemoryDaxConn(StringBuilder buf) {
    this.buf = buf;
  }

  public void append(String text) {
    this.buf.append(text);
  }

  public String get() {
    return this.buf.toString();
  }

  @Override public void commit(AsyncGroup ag) throws Err {
  }

  @Override public boolean isCommitted() {
    return true;
  }

  @Override public void rollback(AsyncGroup ag) {
  }

  @Override public void forceBack(AsyncGroup ag) {
    buf.setLength(0);
  }

  @Override public void close() {
  }
}

interface MemoryDax extends GreetDax, PrintDax, Dax {
  @Override default void output(String text) throws Err {
    MemoryDaxConn conn = getDaxConn("memory");
    conn.append(text);
  }

  @Override default String getText() throws Err {
    MemoryDaxConn conn = getDaxConn("memory");
    return conn.get();
  }
}

