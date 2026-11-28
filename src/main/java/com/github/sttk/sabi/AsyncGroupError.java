/*
 * AsyncGroupError.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

/** Represents error reasons that can occur while running tasks in an {@link AsyncGroup}. */
public sealed interface AsyncGroupError {

  /** Indicates that waiting for an asynchronous task to complete was interrupted. */
  record TaskInterrupted() implements AsyncGroupError {}

  /** Indicates that an {@link Error} or other non-exception {@link Throwable} was thrown. */
  record ErrorOccurred() implements AsyncGroupError {}

  /** Indicates that an unhandled {@link RuntimeException} was thrown during task execution. */
  record RuntimeExceptionOccurred() implements AsyncGroupError {}
}
