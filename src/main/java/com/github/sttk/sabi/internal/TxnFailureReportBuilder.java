/*
 * TxnFailureReport.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.internal;

import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.TxnFailureCause;
import com.github.sttk.sabi.TxnFailureCauseState;
import com.github.sttk.sabi.TxnFailureReport;
import com.github.sttk.sabi.TxnFailureRollback;
import com.github.sttk.sabi.TxnFailureRollbackState;

public class TxnFailureReportBuilder {
  String dataConnName;
  Class<? extends DataConn> dataConnClass;
  TxnFailureCause cause;
  TxnFailureRollback rollback;

  TxnFailureReportBuilder(String name, Class<? extends DataConn> cls) {
    this.dataConnName = name;
    this.dataConnClass = cls;
    this.cause = new TxnFailureCause(TxnFailureCauseState.NoneByUncommitted, null);
    this.rollback = new TxnFailureRollback(TxnFailureRollbackState.NoneByNotRolledBack, null);
  }

  TxnFailureReport build() {
    return new TxnFailureReport(
        this.dataConnName, this.dataConnClass.getName(), this.cause, this.rollback);
  }
}
