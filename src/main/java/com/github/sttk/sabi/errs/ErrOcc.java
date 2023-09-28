/*
 * ErrOcc class.
 * Copyright (C) 2023 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.errs;

import java.time.OffsetDateTime;

/**
 * {@code ErrOcc} is the class which contains time and position in a source
 * file when and where an {@link Err} occured.
 */
public final class ErrOcc {

  /** Time when an {@link Err} occured. */
  private final OffsetDateTime time;

  /** The source file name where an {@link Err} occured. */
  private final String file;

  /** The line number where an {@link Err} occured. */
  private final int line;

  /**
   * The constructor which takes an {@link Err} object as an argument.
   *
   * @param e An {@link Err} object.
   */
  public ErrOcc(final Err e) {
    this.time = OffsetDateTime.now();
    this.file = e.getFileName();
    this.line = e.getLineNumber();
  }

  /**
   * Gets time when an {@link Err} occured.
   *
   * @return  A {@link OffsetDateTime} object.
   */
  public OffsetDateTime getTime() {
    return time;
  }

  /**
   * Gets the source file name where an {@link Err} occured.
   *
   * This source file name can be null if this is unavailable in the stack
   * trace element.
   *
   * @return  A source file name.
   */
  public String getFile() {
    return file;
  }

  /**
   * Gets the line number where an {@link Err} occured.
   *
   * This line number can be a negative number if this is unavailable in the
   * stack trace element.
   *
   * @return  A line number.
   */
  public int getLine() {
    return line;
  }
}

