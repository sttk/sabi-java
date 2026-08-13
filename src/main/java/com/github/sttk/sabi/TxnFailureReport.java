/*
 * TxnFailureReport.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

/**
 * Contains transaction failure diagnostic information and rollback status for a specific data
 * connection.
 *
 * <p>Instances of this class are created during transaction failure processing in {@link DataHub}
 * and passed to {@link DataConn#onTxnFailure(AsyncGroup, java.util.List)}. It details the
 * connection's name, type, failure cause ({@link TxnFailureCause}), and rollback status ({@link
 * TxnFailureRollback}), and provides methods to determine recommended recovery strategies.
 *
 * @since 1.0
 */
public class TxnFailureReport {

  /** The logical name of the data connection. */
  public final String dataConnName;

  /** The class name or type of the data connection. */
  public final String dataConnType;

  /** The cause of the transaction failure associated with this connection. */
  public final TxnFailureCause cause;

  /** The rollback outcome associated with this connection. */
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
   *     {@code NoneByCommitted} or {@code NoneByUncommitted}
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
          case NoneByNotRolledBack:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
          case NoneByRolledBack:
            return TxnFailureRecovery.RerunLogicAndCommit;
          case RollbackFailure:
            return TxnFailureRecovery.ResolveCauseAndInconsistency;
          default:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
        }
      case NoneByCommitted:
        switch (this.rollback.state) {
          case NoneByNotRolledBack:
            return TxnFailureRecovery.NoActionRequired;
          default:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
        }
      case LogicFailure:
        switch (this.rollback.state) {
          case NoneByNotRolledBack:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
          case NoneByRolledBack:
            return TxnFailureRecovery.ResolveCauseThenRerunLogicAndCommit;
          case RollbackFailure:
            return TxnFailureRecovery.ResolveCauseAndInconsistency;
          default:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
        }
      case CommitFailure:
        switch (this.rollback.state) {
          case NoneByNotRolledBack:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
          case NoneByRolledBack:
            return TxnFailureRecovery.ResolveCauseThenRerunLogicAndCommit;
          case RollbackFailure:
            return TxnFailureRecovery.ResolveCauseAndInconsistency;
          default:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
        }
      case PostCommitFailure:
        switch (this.rollback.state) {
          case NoneByNotRolledBack:
            return TxnFailureRecovery.ResolveCauseThenRerunPostCommit;
          default:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
        }
      default:
        return TxnFailureRecovery.InvestigateBecauseImpossible;
    }
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
          case NoneByNotRolledBack:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
          case NoneByRolledBack:
            return TxnFailureRecovery.NoActionRequired;
          case RollbackFailure:
            return TxnFailureRecovery.ResolveCauseAndInconsistency;
          default:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
        }
      case NoneByCommitted:
        switch (this.rollback.state) {
          case NoneByNotRolledBack:
            return TxnFailureRecovery.ManualRollbackRequired;
          default:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
        }
      case LogicFailure:
        switch (this.rollback.state) {
          case NoneByNotRolledBack:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
          case NoneByRolledBack:
            return TxnFailureRecovery.NoActionRequired;
          case RollbackFailure:
            return TxnFailureRecovery.ResolveCauseAndInconsistency;
          default:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
        }
      case CommitFailure:
        switch (this.rollback.state) {
          case NoneByNotRolledBack:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
          case NoneByRolledBack:
            return TxnFailureRecovery.NoActionRequired;
          case RollbackFailure:
            return TxnFailureRecovery.ResolveCauseAndInconsistency;
          default:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
        }
      case PostCommitFailure:
        switch (this.rollback.state) {
          case NoneByNotRolledBack:
            return TxnFailureRecovery.ManualRollbackRequired;
          default:
            return TxnFailureRecovery.InvestigateBecauseImpossible;
        }
      default:
        return TxnFailureRecovery.InvestigateBecauseImpossible;
    }
  }
}
