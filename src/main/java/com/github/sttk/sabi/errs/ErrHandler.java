/*
 * ErrHandler class.
 * Copyright (C) 2022 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.errs;

import java.time.OffsetDateTime;

/**
 * {@code ErrHandler} is a handler of an {@link Err} object creation.
 */
@FunctionalInterface
public interface ErrHandler {

  /**
   * Handles an {@link Err} object creation.
   *
   * @param err  An {@link Err} object.
   * @param occ  An {@link ErrOcc} object which holds the situation parameters
   *   that indicates when and where an {@link Err} occured.
   */
  void handle(Err err, ErrOcc occ);
}
