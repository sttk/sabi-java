/*
 * LogicTxnError.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

/**
 * Represents error reasons recorded when an unhandled {@link RuntimeException} or {@link Error}
 * occurs during a transaction.
 */
public sealed interface LogicTxnError {

  /** Indicates that an unhandled {@link RuntimeException} occurred during transaction execution. */
  record RuntimeExceptionOccurred() implements LogicTxnError {}

  /**
   * Indicates that an {@link Error} or other {@link Throwable} occurred during transaction
   * execution.
   */
  record ErrorOccurred() implements LogicTxnError {}
}
