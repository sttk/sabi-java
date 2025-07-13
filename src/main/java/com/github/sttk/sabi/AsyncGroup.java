/*
 * AsyncGroup class.
 * Copyright (C) 2023-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

public interface AsyncGroup {
  record RunnerFailed() {}

  record RunnerInterrupted() {}

  void add(final Runner runner);
}
