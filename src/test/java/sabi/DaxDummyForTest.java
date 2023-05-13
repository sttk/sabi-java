package sabi;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class DaxDummyForTest {

  static List<String> logs = new ArrayList<>();;

  static boolean willFailToSetUpFooDaxSrc = false;
  static boolean willFailToCommitFooDaxConn = false;
  static boolean willFailToCreateFooDaxConn = false;

  static boolean willFailToCreateBDaxConn = false;
  static boolean willFailToCommitBDaxConn = false;

  static void clearDaxBase() throws Exception {
    DaxAuxForTest.clearGlobalDaxSrcs();

    logs.clear();

    willFailToSetUpFooDaxSrc = false;
    willFailToCommitFooDaxConn = false;
    willFailToCreateFooDaxConn = false;

    willFailToCreateBDaxConn = false;
    willFailToCommitBDaxConn = false;
  }

  static record FailToDoSomething(String text) {}

  static record FailToCreateBDaxConn() {}
  static record FailToCommitBDaxConn() {}
  static record FailToRunLogic() {}

  static class FooDaxConn implements DaxConn {
    final String label;
    final Map<String, String> map = new HashMap<>();

    FooDaxConn(String label) {
      this.label = label;
    }

    @Override
    public void commit() throws Err {
      if (willFailToCommitFooDaxConn) {
        throw new Err(new FailToDoSomething("FailToCommitFooDaxConn"));
      }
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
  }

  static class FooDaxSrc implements DaxSrc {
    String label;

    @Override
    public DaxConn createDaxConn() throws Err {
      if (willFailToCreateFooDaxConn) {
        throw new Err(new FailToDoSomething("FailToCreateFooDaxConn"));
      }
      logs.add("FooDaxSrc#createDaxConn");
      return new FooDaxConn(this.label);
    }

    @Override
    public void setUp() throws Err {
      if (willFailToSetUpFooDaxSrc) {
        throw new Err(new FailToDoSomething("FailToSetUpFooDaxSrc"));
      }
      logs.add("FooDaxSrc#setUp");
    }

    @Override
    public void end() {
      logs.add("FooDaxSrc#end");
    }
  }

  static class BarDaxConn implements DaxConn {
    final String label;
    final Map<String, String> map = new HashMap<>();

    BarDaxConn(String label) {
      this.label = label;
    }

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
  }

  static class BarDaxSrc implements DaxSrc {
    String label;

    @Override
    public DaxConn createDaxConn() throws Err {
      logs.add("BarDaxSrc#createDaxConn");
      return new BarDaxConn(this.label);
    }

    @Override
    public void setUp() throws Err {
      logs.add("BarDaxSrc#setUp");
    }

    @Override
    public void end() {
      logs.add("BarDaxSrc#end");
    }
  }

  static class MapDaxSrc implements DaxSrc {
    final Map<String, String> dataMap = new HashMap<>();

    @Override
    public DaxConn createDaxConn() throws Err {
      return new MapDaxConn(this.dataMap);
    }

    @Override
    public void setUp() throws Err {
    }

    @Override
    public void end() {
    }
  }

  static class MapDaxConn implements DaxConn {
    final Map<String, String> dataMap;

    MapDaxConn(Map<String, String> map) {
      this.dataMap = map;
    }

    @Override
    public void commit() throws Err {
    }

    @Override
    public void rollback() {
    }

    @Override
    public void close() {
    }
  }

  static interface HogeFugaDax extends Dax {
    String getHogeData() throws Err;
    void setFugaData(String data) throws Err;
  }

  static class HogeFugaLogic implements Logic<HogeFugaDax> {
    @Override
    public void run(final HogeFugaDax dax) throws Err {
      var data = dax.getHogeData();
      dax.setFugaData(data);
    }
  }

  static interface FugaPiyoDax extends Dax {
    String getFugaData() throws Err;
    void setPiyoData(String data) throws Err;
  }

  static class FugaPiyoLogic implements Logic<FugaPiyoDax> {
    @Override
    public void run(FugaPiyoDax dax) throws Err {
      var data = dax.getFugaData();
      dax.setPiyoData(data);
    }
  }

  static interface HogeDax extends Dax, HogeFugaDax {
    default String getHogeData() throws Err {
      var conn = getDaxConn("hoge", MapDaxConn.class);
      var data = conn.dataMap.get("hogehoge");
      return data;
    }

    default void SetHogeData(String data) throws Err {
      var conn = getDaxConn("hoge", MapDaxConn.class);
      conn.dataMap.put("hogehoge", data);
    }
  }

  static interface FugaDax extends Dax, HogeFugaDax, FugaPiyoDax {
    default String getFugaData() throws Err {
      var conn = getDaxConn("fuga", MapDaxConn.class);
      var data = conn.dataMap.get("fugafuga");
      return data;
    }

    default void setFugaData(String data) throws Err {
      var conn = getDaxConn("fuga", MapDaxConn.class);
      conn.dataMap.put("fugafuga", data);
    }
  }

  static interface PiyoDax extends Dax, FugaPiyoDax  {
    default String getPiyoData() throws Err {
      var conn = getDaxConn("piyo", MapDaxConn.class);
      var data = conn.dataMap.get("piyopiyo");
      return data;
    }

    default void setPiyoData(String data) throws Err {
      var conn = getDaxConn("piyo", MapDaxConn.class);
      conn.dataMap.put("piyopiyo", data);
    }
  }

  static class HogeFugaPiyoDaxBase extends DaxBase implements
    HogeDax, FugaDax, PiyoDax,
    HogeFugaDax, FugaPiyoDax {}

  static class ADaxSrc implements DaxSrc {
    final Map<String, String> aMap = new HashMap<>();

    @Override
    public DaxConn createDaxConn() throws Err {
      return new ADaxConn(this.aMap);
    }

    @Override
    public void setUp() throws Err {
    }

    @Override
    public void end() {
    }
  }

  static class ADaxConn implements DaxConn {
    final Map<String, String> aMap;

    ADaxConn(Map<String, String> map) {
      this.aMap = map;
    }

    @Override
    public void commit() throws Err {
      logs.add("ADaxConn#commit");
    }

    @Override
    public void rollback() {
      logs.add("ADaxConn#rollback");
    }

    @Override
    public void close() {
      logs.add("ADaxConn#close");
    }
  }

  static class BDaxSrc implements DaxSrc {
    final Map<String, String> bMap = new HashMap<>();

    @Override
    public DaxConn createDaxConn() throws Err {
      if (willFailToCreateBDaxConn) {
        throw new Err(new FailToCreateBDaxConn());
      }
      return new BDaxConn(this.bMap);
    }

    @Override
    public void setUp() throws Err {
    }

    @Override
    public void end() {
    }
  }

  static class BDaxConn implements DaxConn {
    final Map<String, String> bMap;

    BDaxConn(Map<String, String> map) {
      this.bMap = map;
    }

    @Override
    public void commit() throws Err {
      if (willFailToCommitBDaxConn) {
        throw new Err(new FailToCommitBDaxConn());
      }
      logs.add("BDaxConn#commit");
    }

    @Override
    public void rollback() {
      logs.add("BDaxConn#rollback");
    }

    @Override
    public void close() {
      logs.add("BDaxConn#close");
    }
  }

  static class CDaxSrc implements DaxSrc {
    final Map<String, String> cMap = new HashMap<>();

    @Override
    public DaxConn createDaxConn() throws Err {
      return new CDaxConn(this.cMap);
    }

    @Override
    public void setUp() throws Err {
    }

    @Override
    public void end() {
    }
  }

  static class CDaxConn implements DaxConn {
    final Map<String, String> cMap;

    CDaxConn(Map<String, String> map) {
      this.cMap = map;
    }

    @Override
    public void commit() throws Err {
      logs.add("CDaxConn#commit");
    }

    @Override
    public void rollback() {
      logs.add("CDaxConn#rollback");
    }

    @Override
    public void close() {
      logs.add("CDaxConn#close");
    }
  }
}
