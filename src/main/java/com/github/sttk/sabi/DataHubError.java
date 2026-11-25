// Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
// This program is free software under MIT License.
// See the file LICENSE in this distribution for more details.

package com.github.sttk.sabi;

import java.util.List;

public sealed interface DataHubError {

  record FailToSetupGlobalDataSrcs(List<ErrEntry> errors) implements DataHubError {}

  record FailToSetupLocalDataSrcs(List<ErrEntry> errors) implements DataHubError {}

  record NoDataSrcToCreateDataConn(String name, String typeName) implements DataHubError {}

  record FailToRunLogic(List<ErrEntry> errors) implements DataHubError {}

  record AbnormalState() implements DataHubError {}
}
