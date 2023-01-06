/*
 * Proc class.
 * Copyright (C) 2022 Takayuki Sato. All Rights Reserved.
 */
package sabi;

/**
 * Proc is a class which represents a procedure.
 *
 * @param <D> A type of dax used for external data accesses.
 */
public class Proc<D> {

  /**
   * A error reason which indicates a specified dax does not inherit
   * {@link DaxBase} class.
   *
   * @param daxClass  A dax class.
   */
  public record DaxDoesNotInheritDaxBase(Class<?> daxClass) {}

  /** A dax object which has all method interfaces used in this procedure. */
  private final D dax;

  /**
   * A constructor which takes a dax as an argument.
   *
   * The class of the specified dax is needed to inherit {@link DaxBase} class.
   *
   * @param dax  A dax object.
   * @throws ClassCastException - If the dax is not null and does not inherit
   *   {@link DaxBase} class.
   */
  public Proc(final D dax) {
    DaxBase.class.cast(dax);
    this.dax = dax;
  }

  /**
   * Registers a local {@link DaxSrc} with a specified name.
   *
   * @param name  The name for the argument {@link DaxSrc} and also for a
   *   {@link DaxConn} created by the argument {@link DaxSrc}.
   *   This name is used to get a {@link DaxConn} with {@link
   *   DaxBase#getDaxConn} method.
   * @param ds  A {@link DaxSrc} object to be registered locally to enable to
   *   be used in only specific transactions.
   */
  public void addLocalDaxSrc(final String name, final DaxSrc ds) {
    DaxBase.class.cast(dax).addLocalDaxSrc(name, ds);
  }

  /**
   * Runs logic specified as arguments in a transaction.
   *
   * @param logics  Logic functional interfaces.
   * @throws Err  If an exception occurs in a logic.
   */
  @SafeVarargs
  public final void runTxn(final Logic<D> ...logics) throws Err {
    var base = DaxBase.class.cast(dax);

    try {
      base.begin();

      for (var logic : logics) {
        logic.execute(dax);
      }

      base.commit();

    } catch (Err e) {
      base.rollback();
      throw e;

    } finally {
      base.close();
    }
  }

  /**
   * Creates a transaction having specified logics.
   *
   * @param logics  {@link Logic} objects.
   * @return A {@link Runner} object which processes a transaction.
   */
  @SafeVarargs
  public final Runner txn(final Logic<D> ...logics) {
    return new Runner() {
      @Override
      public void run() throws Err {
        var base = DaxBase.class.cast(dax);

        try {
          base.begin();

          for (var logic : logics) {
            logic.execute(dax);
          }

          base.commit();

        } catch (Err e) {
          base.rollback();
          throw e;

        } finally {
          base.close();
        }
      }
    };
  }
}
