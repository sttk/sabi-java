package com.github.sttk.sabi.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.TxnFailureCause;
import com.github.sttk.sabi.TxnFailureCauseState;
import com.github.sttk.sabi.TxnFailureRecovery;
import com.github.sttk.sabi.TxnFailureRollback;
import com.github.sttk.sabi.TxnFailureRollbackState;
import org.junit.jupiter.api.Test;

public class TxnFailureReportBuilderTest {
  private TxnFailureReportBuilderTest() {}

  static class AbcDataConn implements DataConn {
    @Override
    public void commit(AsyncGroup ag) throws Err {}

    @Override
    public boolean isCommitted() {
      return false;
    }

    @Override
    public void rollback(AsyncGroup ag) {}

    @Override
    public void close() {}
  }

  @Test
  void testBuilder() {
    var builder = new TxnFailureReportBuilder("foo", AbcDataConn.class);
    assertThat(builder.dataConnName).isEqualTo("foo");
    assertThat(builder.dataConnClass).isEqualTo(AbcDataConn.class);
    assertThat(builder.cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
    assertThat(builder.cause.err).isNull();
    assertThat(builder.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByNotRolledBack);
    assertThat(builder.rollback.err).isNull();

    var report = builder.build();
    assertThat(report.dataConnName).isEqualTo("foo");
    assertThat(report.dataConnType)
        .isEqualTo("com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn");
    assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
    assertThat(report.cause.err).isNull();
    assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByNotRolledBack);
    assertThat(report.rollback.err).isNull();

    builder.cause = new TxnFailureCause(TxnFailureCauseState.CommitFailure, new Err("fail"));
    builder.rollback =
        new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, new Err("x"));

    report = builder.build();
    assertThat(report.dataConnName).isEqualTo("foo");
    assertThat(report.dataConnType)
        .isEqualTo("com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn");
    assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.CommitFailure);
    assertThat(report.cause.err.getReason()).isEqualTo("fail");
    assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.RollbackFailure);
    assertThat(report.rollback.err.getReason()).isEqualTo("x");
  }

  @Test
  void testIsCauseOfFailure() {
    var builder = new TxnFailureReportBuilder("foo", AbcDataConn.class);
    assertThat(builder.dataConnName).isEqualTo("foo");
    assertThat(builder.dataConnClass).isEqualTo(AbcDataConn.class);
    assertThat(builder.cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
    assertThat(builder.cause.err).isNull();
    assertThat(builder.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByNotRolledBack);
    assertThat(builder.rollback.err).isNull();

    var report = builder.build();
    assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
    assertThat(report.isCauseOfFailure()).isFalse();

    builder.cause = new TxnFailureCause(TxnFailureCauseState.NoneByCommitted, new Err("fail"));
    report = builder.build();
    assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByCommitted);
    assertThat(report.isCauseOfFailure()).isFalse();

    builder.cause = new TxnFailureCause(TxnFailureCauseState.LogicFailure, new Err("fail"));
    report = builder.build();
    assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.LogicFailure);
    assertThat(report.isCauseOfFailure()).isTrue();

    builder.cause = new TxnFailureCause(TxnFailureCauseState.CommitFailure, new Err("fail"));
    report = builder.build();
    assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.CommitFailure);
    assertThat(report.isCauseOfFailure()).isTrue();

    builder.cause = new TxnFailureCause(TxnFailureCauseState.PostCommitFailure, new Err("fail"));
    report = builder.build();
    assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.PostCommitFailure);
    assertThat(report.isCauseOfFailure()).isTrue();
  }

  @Test
  void testRecoveryForCommit() {
    var builder = new TxnFailureReportBuilder("foo", AbcDataConn.class);
    assertThat(builder.dataConnName).isEqualTo("foo");
    assertThat(builder.dataConnClass).isEqualTo(AbcDataConn.class);

    assertThat(builder.cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByNotRolledBack);
      assertThat(report.recoveryForCommit())
          .isEqualTo(TxnFailureRecovery.InvestigateBecauseImpossible);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByRolledBack);
      assertThat(report.recoveryForCommit()).isEqualTo(TxnFailureRecovery.RerunLogicAndCommit);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.RollbackFailure);
      assertThat(report.recoveryForCommit())
          .isEqualTo(TxnFailureRecovery.ResolveCauseAndInconsistency);
    }

    builder.cause = new TxnFailureCause(TxnFailureCauseState.NoneByCommitted, null);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByCommitted);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByNotRolledBack);
      assertThat(report.recoveryForCommit()).isEqualTo(TxnFailureRecovery.NoActionRequired);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByCommitted);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByRolledBack);
      assertThat(report.recoveryForCommit())
          .isEqualTo(TxnFailureRecovery.InvestigateBecauseImpossible);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByCommitted);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.RollbackFailure);
      assertThat(report.recoveryForCommit())
          .isEqualTo(TxnFailureRecovery.InvestigateBecauseImpossible);
    }

    builder.cause = new TxnFailureCause(TxnFailureCauseState.LogicFailure, null);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.LogicFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByNotRolledBack);
      assertThat(report.recoveryForCommit())
          .isEqualTo(TxnFailureRecovery.InvestigateBecauseImpossible);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.LogicFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByRolledBack);
      assertThat(report.recoveryForCommit())
          .isEqualTo(TxnFailureRecovery.ResolveCauseThenRerunLogicAndCommit);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.LogicFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.RollbackFailure);
      assertThat(report.recoveryForCommit())
          .isEqualTo(TxnFailureRecovery.ResolveCauseAndInconsistency);
    }

    builder.cause = new TxnFailureCause(TxnFailureCauseState.CommitFailure, null);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.CommitFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByNotRolledBack);
      assertThat(report.recoveryForCommit())
          .isEqualTo(TxnFailureRecovery.InvestigateBecauseImpossible);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.CommitFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByRolledBack);
      assertThat(report.recoveryForCommit())
          .isEqualTo(TxnFailureRecovery.ResolveCauseThenRerunLogicAndCommit);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.CommitFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.RollbackFailure);
      assertThat(report.recoveryForCommit())
          .isEqualTo(TxnFailureRecovery.ResolveCauseAndInconsistency);
    }

    builder.cause = new TxnFailureCause(TxnFailureCauseState.PostCommitFailure, null);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.PostCommitFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByNotRolledBack);
      assertThat(report.recoveryForCommit())
          .isEqualTo(TxnFailureRecovery.ResolveCauseThenRerunPostCommit);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.PostCommitFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByRolledBack);
      assertThat(report.recoveryForCommit())
          .isEqualTo(TxnFailureRecovery.InvestigateBecauseImpossible);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.PostCommitFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.RollbackFailure);
      assertThat(report.recoveryForCommit())
          .isEqualTo(TxnFailureRecovery.InvestigateBecauseImpossible);
    }
  }

  @Test
  void testRecoveryForRollback() {
    var builder = new TxnFailureReportBuilder("foo", AbcDataConn.class);
    assertThat(builder.dataConnName).isEqualTo("foo");
    assertThat(builder.dataConnClass).isEqualTo(AbcDataConn.class);

    assertThat(builder.cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByNotRolledBack);
      assertThat(report.recoveryForRollback())
          .isEqualTo(TxnFailureRecovery.InvestigateBecauseImpossible);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByRolledBack);
      assertThat(report.recoveryForRollback()).isEqualTo(TxnFailureRecovery.NoActionRequired);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.RollbackFailure);
      assertThat(report.recoveryForRollback())
          .isEqualTo(TxnFailureRecovery.ResolveCauseAndInconsistency);
    }

    builder.cause = new TxnFailureCause(TxnFailureCauseState.NoneByCommitted, null);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByCommitted);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByNotRolledBack);
      assertThat(report.recoveryForRollback()).isEqualTo(TxnFailureRecovery.ManualRollbackRequired);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByCommitted);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByRolledBack);
      assertThat(report.recoveryForRollback())
          .isEqualTo(TxnFailureRecovery.InvestigateBecauseImpossible);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.NoneByCommitted);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.RollbackFailure);
      assertThat(report.recoveryForRollback())
          .isEqualTo(TxnFailureRecovery.InvestigateBecauseImpossible);
    }

    builder.cause = new TxnFailureCause(TxnFailureCauseState.LogicFailure, null);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.LogicFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByNotRolledBack);
      assertThat(report.recoveryForRollback())
          .isEqualTo(TxnFailureRecovery.InvestigateBecauseImpossible);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.LogicFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByRolledBack);
      assertThat(report.recoveryForRollback()).isEqualTo(TxnFailureRecovery.NoActionRequired);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.LogicFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.RollbackFailure);
      assertThat(report.recoveryForRollback())
          .isEqualTo(TxnFailureRecovery.ResolveCauseAndInconsistency);
    }

    builder.cause = new TxnFailureCause(TxnFailureCauseState.CommitFailure, null);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.CommitFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByNotRolledBack);
      assertThat(report.recoveryForRollback())
          .isEqualTo(TxnFailureRecovery.InvestigateBecauseImpossible);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.CommitFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByRolledBack);
      assertThat(report.recoveryForRollback()).isEqualTo(TxnFailureRecovery.NoActionRequired);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.CommitFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.RollbackFailure);
      assertThat(report.recoveryForRollback())
          .isEqualTo(TxnFailureRecovery.ResolveCauseAndInconsistency);
    }

    builder.cause = new TxnFailureCause(TxnFailureCauseState.PostCommitFailure, null);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.PostCommitFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByNotRolledBack);
      assertThat(report.recoveryForRollback()).isEqualTo(TxnFailureRecovery.ManualRollbackRequired);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.PostCommitFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.NoneByRolledBack);
      assertThat(report.recoveryForRollback())
          .isEqualTo(TxnFailureRecovery.InvestigateBecauseImpossible);

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.cause.state).isEqualTo(TxnFailureCauseState.PostCommitFailure);
      assertThat(report.rollback.state).isEqualTo(TxnFailureRollbackState.RollbackFailure);
      assertThat(report.recoveryForRollback())
          .isEqualTo(TxnFailureRecovery.InvestigateBecauseImpossible);
    }
  }

  @Test
  void testToString() {
    var builder = new TxnFailureReportBuilder("foo", AbcDataConn.class);
    assertThat(builder.dataConnName).isEqualTo("foo");
    assertThat(builder.dataConnClass).isEqualTo(AbcDataConn.class);

    assertThat(builder.cause.state).isEqualTo(TxnFailureCauseState.NoneByUncommitted);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}");

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}");

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:NoneByUncommitted Err:null} rollback:{State:RollbackFailure Err:null}}");
    }

    builder.cause = new TxnFailureCause(TxnFailureCauseState.NoneByCommitted, null);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByNotRolledBack Err:null}}");

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:NoneByRolledBack Err:null}}");

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:NoneByCommitted Err:null} rollback:{State:RollbackFailure Err:null}}");
    }

    builder.cause = new TxnFailureCause(TxnFailureCauseState.LogicFailure, null);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:LogicFailure Err:null} rollback:{State:NoneByNotRolledBack Err:null}}");

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:LogicFailure Err:null} rollback:{State:NoneByRolledBack Err:null}}");

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:LogicFailure Err:null} rollback:{State:RollbackFailure Err:null}}");
    }

    builder.cause = new TxnFailureCause(TxnFailureCauseState.CommitFailure, null);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:CommitFailure Err:null} rollback:{State:NoneByNotRolledBack Err:null}}");

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:CommitFailure Err:null} rollback:{State:NoneByRolledBack Err:null}}");

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:CommitFailure Err:null} rollback:{State:RollbackFailure Err:null}}");
    }

    builder.cause = new TxnFailureCause(TxnFailureCauseState.PostCommitFailure, null);
    {
      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
      var report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:PostCommitFailure Err:null} rollback:{State:NoneByNotRolledBack Err:null}}");

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByRolledBack, null);
      report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:PostCommitFailure Err:null} rollback:{State:NoneByRolledBack Err:null}}");

      builder.rollback = new TxnFailureRollback(TxnFailureRollbackState.RollbackFailure, null);
      report = builder.build();
      assertThat(report.toString())
          .isEqualTo(
              "{dataConnName:foo dataConnType:com.github.sttk.sabi.internal.TxnFailureReportBuilderTest$AbcDataConn cause:{State:PostCommitFailure Err:null} rollback:{State:RollbackFailure Err:null}}");
    }
  }
}
