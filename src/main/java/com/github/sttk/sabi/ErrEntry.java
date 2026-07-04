/*
 * ErrEntry.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi;

import com.github.sttk.errs.Err;

/**
 * Represents an error entry containing details of a failure associated with a specific data source
 * or data connection.
 *
 * <p>Instances of this class record the positional index, logical name, and cause of an error when
 * batch operations (such as global/local data source setup or connection pre-commit, commit, and
 * post-commit phases) encounter errors across multiple connections.
 */
public class ErrEntry {

  /** The positional index of the data source or connection in the operation sequence. */
  public final int index;

  /** The logical name of the data source or connection where the error occurred. */
  public final String name;

  /** The error instance containing details of the failure cause. */
  public final Err err;

  /**
   * Constructs a new {@code ErrEntry} with the specified index, name, and error cause.
   *
   * @param index the positional index of the data source or connection
   * @param name the logical name of the data source or connection
   * @param err the error instance detailing the failure cause
   */
  public ErrEntry(int index, String name, Err err) {
    this.index = index;
    this.name = name;
    this.err = err;
  }
}
