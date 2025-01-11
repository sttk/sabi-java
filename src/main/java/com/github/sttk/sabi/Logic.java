/*
 * Logic class.
 * Copyright (C) 2022-2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Exc;

@FunctionalInterface
public interface Logic<D> {
  void run(D data) throws Exc;
}
