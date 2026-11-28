package com.github.sttk.sabi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TestUtils {
  private TestUtils() {}

  public static String blackOutLineNo(String s) {
    return s.replaceAll("line = [0-9]+", "line = ***");
  }

  // Because the ClassCastException message could vary depending on the environment.
  public static String replaceClassCastExceptionMessage(String s) {
    return s.replaceAll(
        " java.lang.ClassCastException: Cannot cast ([^ ]+) to ",
        " java.lang.ClassCastException: $1 cannot be cast to ");
  }

  @Nested
  class TestBlackOutLineNo {
    @Test
    void testSimpleReplacement_singleReplacement() {
      assertThat(blackOutLineNo("line = 123")).isEqualTo("line = ***");
    }

    @Test
    void testSimpleReplacement_multipleReplacement() {
      assertThat(blackOutLineNo("{line = 123}, { line = 987 }"))
          .isEqualTo("{line = ***}, { line = *** }");
    }

    @Test
    void testWithActualExample_singleReplacement() {
      assertThat(
              blackOutLineNo(
                  "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestDoubles.java, line = 58 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]"))
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:CommitFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ZZZ, file = TestDoubles.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}]");
    }

    @Test
    void testWithActualExample_multipleReplacement() {
      assertThat(
              blackOutLineNo(
                  "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = 869 }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = 869 }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestDoubles.java, line = 113 }}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = 869 }} rollback:{State:NoneByRolledBack Err:null}}]"))
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = *** }} rollback:{State:RollbackFailure Err:com.github.sttk.errs.Err { reason = java.lang.String ???, file = TestDoubles.java, line = *** }}}, {dataConnName:baz dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = java.lang.String fail, file = LogicRunnerTest.java, line = *** }} rollback:{State:NoneByRolledBack Err:null}}]");
    }
  }

  @Nested
  class TestReplaceClassCastExceptionMessage {
    @Test
    void testSimpleReplacement_singleReplacement() {
      assertThat(
              replaceClassCastExceptionMessage(
                  "cause = java.lang.ClassCastException: Cannot cast com.github.sttk.sabi.TestDoubles$SyncDataConn to com.github.sttk.sabi.TestDoubles$AsyncDataConn"))
          .isEqualTo(
              "cause = java.lang.ClassCastException: com.github.sttk.sabi.TestDoubles$SyncDataConn cannot be cast to com.github.sttk.sabi.TestDoubles$AsyncDataConn");
    }

    @Test
    void testWithActualExample_multipleReplacement() {
      assertThat(
              replaceClassCastExceptionMessage(
                  "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataConnError$FailToCastDataConn FailToCastDataConn[name=bar, fromTypeName=com.github.sttk.sabi.TestDoubles$SyncDataConn, toTypeName=com.github.sttk.sabi.TestDoubles$AsyncDataConn], file = DataConnManager.java, line = ***, cause = java.lang.ClassCastException: Cannot cast com.github.sttk.sabi.TestDoubles$SyncDataConn to com.github.sttk.sabi.TestDoubles$AsyncDataConn }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataConnError$FailToCastDataConn FailToCastDataConn[name=bar, fromTypeName=com.github.sttk.sabi.TestDoubles$SyncDataConn, toTypeName=com.github.sttk.sabi.TestDoubles$AsyncDataConn], file = DataConnManager.java, line = ***, cause = java.lang.ClassCastException: Cannot cast com.github.sttk.sabi.TestDoubles$SyncDataConn to com.github.sttk.sabi.TestDoubles$AsyncDataConn }} rollback:{State:NoneByRolledBack Err:null}}]"))
          .isEqualTo(
              "TxnFailureReports=[{dataConnName:foo dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataConnError$FailToCastDataConn FailToCastDataConn[name=bar, fromTypeName=com.github.sttk.sabi.TestDoubles$SyncDataConn, toTypeName=com.github.sttk.sabi.TestDoubles$AsyncDataConn], file = DataConnManager.java, line = ***, cause = java.lang.ClassCastException: com.github.sttk.sabi.TestDoubles$SyncDataConn cannot be cast to com.github.sttk.sabi.TestDoubles$AsyncDataConn }} rollback:{State:NoneByRolledBack Err:null}}, {dataConnName:bar dataConnType:com.github.sttk.sabi.TestDoubles$SyncDataConn cause:{State:LogicFailure Err:com.github.sttk.errs.Err { reason = com.github.sttk.sabi.DataConnError$FailToCastDataConn FailToCastDataConn[name=bar, fromTypeName=com.github.sttk.sabi.TestDoubles$SyncDataConn, toTypeName=com.github.sttk.sabi.TestDoubles$AsyncDataConn], file = DataConnManager.java, line = ***, cause = java.lang.ClassCastException: com.github.sttk.sabi.TestDoubles$SyncDataConn cannot be cast to com.github.sttk.sabi.TestDoubles$AsyncDataConn }} rollback:{State:NoneByRolledBack Err:null}}]");
    }
  }
}
