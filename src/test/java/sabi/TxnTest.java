package sabi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static sabi.DaxAuxForTest.*;
import static sabi.DaxDummyForTest.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;

public class TxnTest {

  @BeforeEach
  void clear() throws Exception {
    clearDaxBase();
  }

  static interface ABDax {
    String getAData() throws Err;
    void setBData(String data) throws Err;
  }

  static interface AGetDax extends ABDax, Dax {
    default String getAData() throws Err {
      var conn = getDaxConn("aaa", ADaxConn.class);
      var data = conn.aMap.get("a");
      return data;
    }
  }

  static interface BGetSetDax extends ABDax, Dax {
    default String getBData() throws Err {
      var conn = getDaxConn("bbb", BDaxConn.class);
      var data = conn.bMap.get("b");
      return data;
    }

    default void setBData(String data) throws Err {
      var conn = getDaxConn("bbb", BDaxConn.class);
      conn.bMap.put("b", data);
    }
  }

  static interface CSetDax extends ABDax, Dax {
    default void setCData(String data) throws Err {
      var conn = getDaxConn("ccc", CDaxConn.class);
      conn.cMap.put("c", data);
    }
  }

  static class ABDaxBase extends DaxBase
    implements ABDax, AGetDax, BGetSetDax, CSetDax {}

  @Test
  void txnRun() throws Exception {
    var base = new ABDaxBase();

    var aDs = new ADaxSrc();
    var bDs = new BDaxSrc();

    try {
      base.setUpLocalDaxSrc("aaa", aDs);
      base.setUpLocalDaxSrc("bbb", bDs);

      aDs.aMap.put("a", "hello");

      Txn.run(base, (ABDax dax) -> {
        var data = dax.getAData();
        data = data.toUpperCase();
        dax.setBData(data);
      });

    } catch (Err e) {
      fail(e);
    }
  }
}
