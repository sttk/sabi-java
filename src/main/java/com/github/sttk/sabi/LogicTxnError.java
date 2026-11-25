// Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
// This program is free software under MIT License.
// See the file LICENSE in this distribution for more details.

package com.github.sttk.sabi;

public sealed interface LogicTxnError {

  record RuntimeExceptionOccurred() implements LogicTxnError {}

  record ErrorOccurred() implements LogicTxnError {}
}
