/*
 * TxnFailureReport.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

public final class TxnFailureReport {

  public final String dataConnName;
  public final String dataConnType;
  public final TxnFailureCause cause;
  public final TxnFailureRollback rollback;

  public TxnFailureReport(
      String name, String type, TxnFailureCause cause, TxnFailureRollback rollback) {
    this.dataConnName = name;
    this.dataConnType = type;
    this.cause = cause;
    this.rollback = rollback;
  }

  public String toString() {
    var buf = new StringBuilder("{");
    buf.append("dataConnName:").append(this.dataConnName);
    buf.append(" ");
    buf.append("dataConnType:").append(this.dataConnType);
    buf.append(" ");
    buf.append("cause:").append(this.cause);
    buf.append(" ");
    buf.append("rollback:").append(this.rollback);
    buf.append("}");
    return buf.toString();
  }

  public boolean isCauseOfFailure() {
    switch (this.cause.state) {
      case NoneByCommitted:
      case NoneByUncommitted:
        return false;
      default:
        return true;
    }
  }

  public TxnFailureRecovery recoveryForCommit() {
    switch (this.cause.state) {
      case NoneByUncommitted:
        switch (this.rollback.state) {
          case NoneByRolledBack:
            return TxnFailureRecovery.RerunLogicAndCommit;
          case RollbackFailure:
            return TxnFailureRecovery.ResolveCauseAndInconsistency;
        }
        break;
      case NoneByCommitted:
        switch (this.rollback.state) {
          case NoneByNotRolledBack:
            return TxnFailureRecovery.NoActionRequired;
        }
        break;
      case LogicFailure:
      case PreCommitFailure:
      case CommitFailure:
        switch (this.rollback.state) {
          case NoneByRolledBack:
            return TxnFailureRecovery.ResolveCauseThenRerunLogicAndCommit;
          case RollbackFailure:
            return TxnFailureRecovery.ResolveCauseAndInconsistency;
        }
        break;
      default: // case PostCommitFailure:
        switch (this.rollback.state) {
          case NoneByNotRolledBack:
            return TxnFailureRecovery.ResolveCauseThenRerunPostCommit;
        }
        break;
    }
    return TxnFailureRecovery.InvestigateBecauseImpossible;
  }

  public TxnFailureRecovery recoveryForRollback() {
    switch (this.cause.state) {
      case NoneByUncommitted:
        switch (this.rollback.state) {
          case NoneByRolledBack:
            return TxnFailureRecovery.NoActionRequired;
          case RollbackFailure:
            return TxnFailureRecovery.ResolveCauseAndInconsistency;
        }
        break;
      case NoneByCommitted:
        switch (this.rollback.state) {
          case NoneByNotRolledBack:
            return TxnFailureRecovery.ManualRollbackRequired;
        }
        break;
      case LogicFailure:
      case PreCommitFailure:
      case CommitFailure:
        switch (this.rollback.state) {
          case NoneByRolledBack:
            return TxnFailureRecovery.NoActionRequired;
          case RollbackFailure:
            return TxnFailureRecovery.ResolveCauseAndInconsistency;
        }
        break;
      default: // case PostCommitFailure:
        switch (this.rollback.state) {
          case NoneByNotRolledBack:
            return TxnFailureRecovery.ManualRollbackRequired;
        }
        break;
    }
    return TxnFailureRecovery.InvestigateBecauseImpossible;
  }

  static class Builder {
    String dataConnName;
    Class<? extends DataConn> dataConnClass;
    TxnFailureCause cause;
    TxnFailureRollback rollback;

    Builder(String name, Class<? extends DataConn> cls, Err logicErr) {
      this.dataConnName = name;
      this.dataConnClass = cls;
      if (logicErr == null) {
        this.cause = new TxnFailureCause(TxnFailureCauseState.NoneByUncommitted, null);
      } else {
        this.cause = new TxnFailureCause(TxnFailureCauseState.LogicFailure, logicErr);
      }
      this.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
    }

    TxnFailureReport build() {
      return new TxnFailureReport(
          this.dataConnName, this.dataConnClass.getName(), this.cause, this.rollback);
    }
  }
}
