/*
 * ErrHandler class.
 * Copyright (C) 2022 Takayuki Sato. All Rights Reserved.
 */
package sabi;

import java.time.OffsetDateTime;

/**
 * This class is a handler of an {@link Err} object creation.
 */
@FunctionalInterface
public interface ErrHandler {

  /**
   * Handles an {@link Err} object which will be created rigth after.
   *
   * @param err  An {@link Err} object.
   * @param occ  An {@link ErrOccasion} object which holds when and where
   *   an Err occured.
   */
  void handle(Err err, ErrOccasion occ);
}
