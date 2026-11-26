// Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
// This program is free software under MIT License.
// See the file LICENSE in this distribution for more details.

package com.github.sttk.sabi;

/**
 * Represents error reasons that occur when creating or casting a {@link DataConn} from a {@link
 * DataSrc}.
 */
public sealed interface DataSrcError {

  /**
   * Indicates that a newly created data connection could not be cast to the requested type.
   *
   * @param name the logical name of the data source
   * @param fromTypeName the actual class name of the created data connection
   * @param toTypeName the target class name requested by the caller
   */
  record FailToCastCreatedDataConn(String name, String fromTypeName, String toTypeName)
      implements DataSrcError {}

  /**
   * Indicates that {@link DataSrc#createDataConn()} threw an exception.
   *
   * @param name the logical name of the data source
   * @param typeName the target data connection class name
   */
  record FailToCreateDataConn(String name, String typeName) implements DataSrcError {}

  /**
   * Indicates that {@link DataSrc#createDataConn()} returned {@code null}.
   *
   * @param name the logical name of the data source
   * @param typeName the target data connection class name
   */
  record CreateDataConnIsNull(String name, String typeName) implements DataSrcError {}
}
