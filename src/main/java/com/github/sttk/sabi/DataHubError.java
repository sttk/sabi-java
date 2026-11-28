/*
 * DataHubError.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import java.util.List;

/**
 * Represents error reasons produced by {@link DataHub}, {@link Globals}, {@link LogicRunner},
 * {@link LogicRun}, and {@link LogicTxn} operations.
 */
public sealed interface DataHubError {

  /**
   * Indicates that setting up one or more global data sources failed.
   *
   * @param errors the list of error entries collected during global data source setup
   */
  record FailToSetupGlobalDataSrcs(List<ErrEntry> errors) implements DataHubError {}

  /**
   * Indicates that setting up one or more local data sources failed.
   *
   * @param errors the list of error entries collected during local data source setup
   */
  record FailToSetupLocalDataSrcs(List<ErrEntry> errors) implements DataHubError {}

  /**
   * Indicates that no data source was found for the requested data connection name.
   *
   * @param name the logical name of the requested data source
   * @param typeName the expected class name of the data connection
   */
  record NoDataSrcToCreateDataConn(String name, String typeName) implements DataHubError {}

  /**
   * Indicates that one or more logic executions failed within a {@link LogicRun} or {@link
   * LogicTxn}.
   *
   * @param errors the list of error entries produced by the failed logic executions
   */
  record FailToRunLogic(List<ErrEntry> errors) implements DataHubError {}

  /**
   * Indicates that the {@link DataHub} is in an abnormal state after an unhandled {@link
   * RuntimeException} or {@link Error} caused an abnormal termination.
   */
  record AbnormalState() implements DataHubError {}
}
