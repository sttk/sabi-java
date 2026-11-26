/*
 * TxnFailureReport.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

/**
 * Reports the failure cause and rollback outcome for a single {@link DataConn} when a transaction
 * fails.
 *
 * <p>Instances are passed to {@link DataConn#onTxnFailure(AsyncGroup, java.util.List)} to provide
 * diagnostics and help determine recovery strategies via {@link #recoveryForCommit()} and {@link
 * #recoveryForRollback()}.
 */
public final class TxnFailureReport {

  /** The logical name of the data connection. */
  public final String dataConnName;

  /** The fully qualified class name of the data connection. */
  public final String dataConnType;

  /** The cause of the transaction failure for this connection. */
  public final TxnFailureCause cause;

  /** The rollback outcome for this connection. */
  public final TxnFailureRollback rollback;

  /**
   * Constructs a new {@code TxnFailureReport} with the specified connection details, cause, and
   * rollback outcome.
   *
   * @param name the logical name of the data connection
   * @param type the class name or type of the data connection
   * @param cause the transaction failure cause
   * @param rollback the rollback outcome
   */
  public TxnFailureReport(
      String name, String type, TxnFailureCause cause, TxnFailureRollback rollback) {
    this.dataConnName = name;
    this.dataConnType = type;
    this.cause = cause;
    this.rollback = rollback;
  }

  /**
   * Returns a string representation of this transaction failure report.
   *
   * @return a formatted string containing connection details, cause, and rollback outcome
   */
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

  /**
   * Determines whether this connection was a direct cause of the transaction failure.
   *
   * @return {@code true} if this connection caused the failure; {@code false} if the cause state is
   *     {@link TxnFailureCauseState#NoneByCommitted} or {@link
   *     TxnFailureCauseState#NoneByUncommitted}
   */
  public boolean isCauseOfFailure() {
    switch (this.cause.state) {
      case NoneByCommitted:
      case NoneByUncommitted:
        return false;
      default:
        return true;
    }
  }

  /**
   * Determines the recommended recovery strategy if the intent is to re-attempt and complete the
   * transaction commit.
   *
   * @return the recommended {@link TxnFailureRecovery} action for committing
   */
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

  /**
   * Determines the recommended recovery strategy if the intent is to cancel and roll back the
   * transaction.
   *
   * @return the recommended {@link TxnFailureRecovery} action for rolling back
   */
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
