/*
 * ErrEntry.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

public record ErrEntry(int index, String name, Err err) {}
