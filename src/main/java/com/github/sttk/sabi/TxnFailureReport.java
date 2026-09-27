/*
 * TxnFailureReport.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

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
        switch (this.rollback.state) {
          case NoneByRolledBack:
            return TxnFailureRecovery.ResolveCauseThenRerunLogicAndCommit;
          case RollbackFailure:
            return TxnFailureRecovery.ResolveCauseAndInconsistency;
        }
        break;
      case CommitFailure:
        switch (this.rollback.state) {
          case NoneByRolledBack:
            return TxnFailureRecovery.ResolveCauseThenRerunLogicAndCommit;
          case RollbackFailure:
            return TxnFailureRecovery.ResolveCauseAndInconsistency;
        }
        break;
      case PostCommitFailure:
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
        switch (this.rollback.state) {
          case NoneByRolledBack:
            return TxnFailureRecovery.NoActionRequired;
          case RollbackFailure:
            return TxnFailureRecovery.ResolveCauseAndInconsistency;
        }
        break;
      case CommitFailure:
        switch (this.rollback.state) {
          case NoneByRolledBack:
            return TxnFailureRecovery.NoActionRequired;
          case RollbackFailure:
            return TxnFailureRecovery.ResolveCauseAndInconsistency;
        }
        break;
      case PostCommitFailure:
        switch (this.rollback.state) {
          case NoneByNotRolledBack:
            return TxnFailureRecovery.ManualRollbackRequired;
        }
        break;
    }
    return TxnFailureRecovery.InvestigateBecauseImpossible;
  }
}
