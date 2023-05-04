/*
 * Txn class.
 * Copyright (C) 2023 Takayuki Sato. All Rights Reserved.
 */
package sabi;

/**
 * Txn is a class to run a transaction process.
 */
public class Txn<D> implements Runner {

  /**
   * The {@link DaxBase} object which holds {@link DaxSrc}(s) and
   * {@link DaxConn}(s), and has data access methods used in the logics.
   */
  private final DaxBase base;

  /** The logics to be run in this transaction. */
  private final Logic<D>[] logics;

  /**
   * The constructor which takes logics to be run in this transaction.
   *
   * @param base  A {@link DaxBase} for a transaction process and data access
   *   methods.
   * @param logics  {@link Logic}'s variadic arguments.
   */
  @SafeVarargs
  public Txn(DaxBase base, Logic<D> ...logics) {
    this.base = base;
    this.logics = logics;
  }

  /**
   * Runs the {@link Logic}(s) holding in this object in a transaction.
   *
   * @throws Err  If it is failed to run one of the logics.
   */
  @Override
  public void run() throws Err {
    run(this.base, this.logics);
  }

  /**
   * Runs the specified {@link Logic}(s) with the {@link DaxBase} in a
   * transaction.
   *
   * @param base  A {@link DaxBase} for a transaction process and data access
   *   methods.
   * @param logics  {@link Logic}'s variadic arguments.
   * @throws Err  If it is failed to run one of the logics.
   */
  @SafeVarargs
  public static <D> void run(DaxBase base, Logic<D> ...logics) throws Err {
    try {
      base.begin();

      @SuppressWarnings("unchecked")
      final D dax = (D) base;

      for (var logic : logics) {
        logic.run(dax);
      }

      base.commit();

    } catch (Err e) {
      base.rollback();
      throw e;

    } finally {
      base.end();
    }
  }
}
