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

  static interface AGetDax extends ABDax, ADax {
    default String getAData() throws Err {
      var conn = getADaxConn("aaa");
      var data = conn.aMap.get("a");
      return data;
    }
  }

  static interface BGetSetDax extends ABDax, BDax {
    default String getBData() throws Err {
      var conn = getBDaxConn("bbb");
      var data = conn.bMap.get("b");
      return data;
    }

    default void setBData(String data) throws Err {
      var conn = getBDaxConn("bbb");
      conn.bMap.put("b", data);
    }
  }

  static interface CSetDax extends ABDax, CDax {
    default void setCData(String data) throws Err {
      var conn = getCDaxConn("ccc");
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
