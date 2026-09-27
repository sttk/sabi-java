/*
 * AsyncGroup.java
 * Copyright (C) 2023-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.sabi.internal.AsyncGroupImpl;

public abstract sealed class AsyncGroup permits AsyncGroupImpl {
  public record FunctionFailed() {}

  public record FunctionInterrupted() {}

  public record RuntimeExceptionOccured() {}

  protected AsyncGroup() {}

  public abstract void add(Function fn);
}
