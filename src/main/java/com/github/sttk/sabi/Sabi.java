/*
 * Sabi class.
 * Copyright (C) 2022-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.internal.DataHubInner;

public final class Sabi {
  private Sabi() {}

  public static void uses(String name, DataSrc ds) {
    DataHubInner.usesGlobal(name, ds);
  }

  public static AutoCloseable setup() throws Exc {
    return DataHubInner.setupGlobals();
  }
}
