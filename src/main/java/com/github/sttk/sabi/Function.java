/*
 * Function.java
 * Copyright (C) 2022-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

@FunctionalInterface
public interface Function {

  void apply() throws Err;
}
