/*
 * AsyncGroupError.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

public sealed interface AsyncGroupError {

  record TaskInterrupted() implements AsyncGroupError {}

  record ErrorOccurred() implements AsyncGroupError {}

  record RuntimeExceptionOccurred() implements AsyncGroupError {}
}
