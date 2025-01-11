/*
 * AsyncGroup class.
 * Copyright (C) 2023-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Exc;

import java.util.Map;
import java.util.HashMap;

public interface AsyncGroup {
  record RunnerFailed() {}
  record RunnerInterrupted() {}

  void add(final Runner runner);
}
