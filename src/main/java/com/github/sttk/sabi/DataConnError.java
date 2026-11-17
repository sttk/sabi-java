/*
 * DataConnError.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import java.util.List;

public sealed interface DataConnError {

  public record FailToPreCommitDataConn(List<ErrEntry> errors) implements DataConnError {}

  public record FailToCommitDataConn(List<ErrEntry> errors) implements DataConnError {}

  public record FailToPostCommitDataConn(List<ErrEntry> errors) implements DataConnError {}
}
