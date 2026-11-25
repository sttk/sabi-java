// Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
// This program is free software under MIT License.
// See the file LICENSE in this distribution for more details.

package com.github.sttk.sabi;

public sealed interface DataSrcError {
  record FailToCastCreatedDataConn(String name, String fromTypeName, String toTypeName)
      implements DataSrcError {}

  record FailToCreateDataConn(String name, String typeName) implements DataSrcError {}

  record CreateDataConnIsNull(String name, String typeName) implements DataSrcError {}
}
