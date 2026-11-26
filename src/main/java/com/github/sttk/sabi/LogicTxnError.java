// Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
// This program is free software under MIT License.
// See the file LICENSE in this distribution for more details.

package com.github.sttk.sabi;

/**
 * Represents error reasons recorded when an unhandled {@link RuntimeException} or {@link Error}
 * occurs during a transaction.
 */
public sealed interface LogicTxnError {

  /** Indicates that an unhandled {@link RuntimeException} occurred during transaction execution. */
  record RuntimeExceptionOccurred() implements LogicTxnError {}

  /** Indicates that an {@link Error} or other {@link Throwable} occurred during transaction execution. */
  record ErrorOccurred() implements LogicTxnError {}
}
