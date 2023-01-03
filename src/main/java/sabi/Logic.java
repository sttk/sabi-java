/*
 * Logic class.
 * Copyright (C) 2022 Takayuki Sato. All Rights Reserved.
 */
package sabi;

/**
 * Logic is a functional interface which executes a logical process.
 *
 * In this class, only logical codes should be written and data access codes
 * for external data sources should not.
 * Data access codes should be written in methods associated with a dax
 * interface which is an argument of {@link #execute} method.
 */
@FunctionalInterface
public interface Logic<D> {

  /**
   * Executes the logical process represented by this class.
   *
   * This method is the entry point of the whole logical process represented by
   * this class.
   *
   * @param dax  A data access interface.
   * @throws Err  If an error occured in an implementation for an argument dax.
   */
  void execute(final D dax) throws Err;
}
