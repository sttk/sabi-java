/*
 * DataConnError.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import java.util.List;

/**
 * Represents error reasons related to {@link DataConn} operations such as committing and casting.
 */
public sealed interface DataConnError {

  /**
   * Indicates that one or more data connections failed during the pre-commit phase.
   *
   * @param errors the list of error entries detailing connection names and failure causes
   */
  public record FailToPreCommitDataConn(List<ErrEntry> errors) implements DataConnError {}

  /**
   * Indicates that one or more data connections failed during the commit phase.
   *
   * @param errors the list of error entries detailing connection names and failure causes
   */
  public record FailToCommitDataConn(List<ErrEntry> errors) implements DataConnError {}

  /**
   * Indicates that one or more data connections failed during the post-commit phase.
   *
   * @param errors the list of error entries detailing connection names and failure causes
   */
  public record FailToPostCommitDataConn(List<ErrEntry> errors) implements DataConnError {}

  /**
   * Indicates that a cached data connection could not be cast to the requested type.
   *
   * @param name the logical name of the data connection
   * @param fromTypeName the actual class name of the data connection
   * @param toTypeName the target class name requested by the caller
   */
  public record FailToCastDataConn(String name, String fromTypeName, String toTypeName)
      implements DataConnError {}
}
