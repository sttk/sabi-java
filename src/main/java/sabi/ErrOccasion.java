/*
 * ErrOccasion class.
 * Copyright (C) 2023 Takayuki Sato. All Rights Reserved.
 */
package sabi;

import java.time.OffsetDateTime;

/**
 * ErrOccasion is a class which contains time and position in a source file
 * when and where an Err occured.
 */
public final class ErrOccasion {

  /** Time when an Err occured. */
  private final OffsetDateTime time;

  /** The source file name where an Err occured. */
  private final String file;

  /** The line number where an Err occured. */
  private final int line;

  /**
   * The constructor which takes an Err object.
   *
   * @param  err An Err object.
   */
  public ErrOccasion(final Err err) {
    this.time = OffsetDateTime.now();
    this.file = err.getFileName();
    this.line = err.getLineNumber();
  }

  /**
   * Gets time when an Err occured.
   *
   * @return  A {@link OffsetDateTime} object.
   */
  public OffsetDateTime getTime() {
    return time;
  }

  /**
   * Gets a source file name where an Err occured.
   *
   * This source file name is null if this is unavailable in the stack trace
   * element.
   *
   * @return  A source file name.
   */
  public String getFile() {
    return file;
  }

  /**
   * Gets a line number where an Err occured.
   *
   * This line number is a nevative number if this is unavailable in the stack
   * trace element.
   *
   * @return  A line number.
   */
  public int getLine() {
    return line;
  }
}
