/*
 * ErrEntry.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

/**
 * Associates an {@link Err} with the positional index and name of the data source, data
 * connection, or logic invocation where the error occurred.
 *
 * @param index the zero-based positional index of the item that failed
 * @param name the logical name or invocation label associated with the failure
 * @param err the {@link Err} instance detailing the failure cause
 */
public record ErrEntry(int index, String name, Err err) {}
