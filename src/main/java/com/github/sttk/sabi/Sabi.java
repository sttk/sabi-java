/*
 * Sabi.java
 * Copyright (C) 2022-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.internal.GlobalSetup;
import java.util.List;

public final class Sabi {
  private Sabi() {}

  public record FailToSetupGlobalDataSrcs(List<ErrEntry> errors) {}

  public static void uses(String name, DataSrc ds) {
    GlobalSetup.uses(name, ds);
  }

  public static AutoCloseable setup() throws Err {
    return GlobalSetup.setup();
  }

  public static AutoCloseable setup(String... names) throws Err {
    return GlobalSetup.setupWithOrder(List.of(names));
  }

  public static AutoCloseable setup(List<String> names) throws Err {
    return GlobalSetup.setupWithOrder(names);
  }
}
