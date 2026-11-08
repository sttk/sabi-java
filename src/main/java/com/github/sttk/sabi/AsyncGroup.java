/*
 * AsyncGroup.java
 * Copyright (C) 2023-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.internal.AsyncGroupImpl;

public abstract sealed class AsyncGroup permits AsyncGroupImpl {
  public record TaskInterrupted() {}

  public record ErrorOccurred() {}

  public record RuntimeExceptionOccurred() {}

  protected AsyncGroup() {}

  public abstract void add(Task task);

  @FunctionalInterface
  public interface Task {
    void execute() throws Err;
  }
}
