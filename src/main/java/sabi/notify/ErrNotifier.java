/*
 * ErrNotifier class.
 * Copyright (C) 2022-2023 Takayuki Sato. All Rights Reserved.
 */
package sabi.notify;

import sabi.Err;
import sabi.ErrHandler;
import sabi.ErrOccasion;

import java.util.List;
import java.util.LinkedList;

/**
 * This class notifies {@link Err} object creations to {@link ErrHandler}.
 * This class manages a list for handlers which is processed synchronously and
 * another list for handlers which is processed asynchronously.
 *
 * The only one instance of this class is created as a static field of {@link
 * Err} class.
 */
public final class ErrNotifier {

  /** The flag meaning whether this instance is fixed or not. */
  private boolean isFixed = false;

  /**
   * The list which holds {@link ErrHandler} objects which is executed
   * synchronously.
   */
  protected final List<ErrHandler> syncErrHandlers = new LinkedList<>();


  /**
   * The list which holds {@link ErrHandler} objects which is executed
   * asynchronously.
   */
  protected final List<ErrHandler> asyncErrHandlers = new LinkedList<>();


  /**
   * Constructs an instance of this class with no argument.
   */
  public ErrNotifier() {}


  /**
   * Checks whether this object is fixed or not.
   */
  public boolean isFixed() {
    return this.isFixed;
  }


  /**
   * Registers an {@link ErrHandler} object which is processed synchronously.
   * After calling {@link #fix}, this method registers no more.
   *
   * @param handler  An {@link ErrHandler} object.
   */
  public synchronized void addSyncErrHandler(final ErrHandler handler) {
    if (this.isFixed) {
      return;
    }
    this.syncErrHandlers.add(handler);
  }


  /**
   * Registers an {@link ErrHandler} object which is processed asynchronously.
   * After calling {@link #fix}, this method registers no more.
   *
   * @param handler  An {@link ErrHandler} object.
   */
  public synchronized void addAsyncErrHandler(final ErrHandler handler) {
    if (this.isFixed) {
      return;
    }
    this.asyncErrHandlers.add(handler);
  }


  /**
   * Fixes this object.
   * This method makes it impossible to add more {@link ErrHandler}s to this
   * object, and possible to notify that an {@link Err} object is created.
   */
  public synchronized void fix() {
    this.isFixed = true;
  }


  /**
   * Notifies that an {@link Err} object is created.
   * However, this method does nothing until this object is fixed.
   */
  public void notify(final Err err) {
    if (!this.isFixed) {
      return;
    }

    final var errOcc = new ErrOccasion(err);

    for (var handler : this.syncErrHandlers) {
      handler.handle(err, errOcc);
    }

    if (!this.asyncErrHandlers.isEmpty()) {
      final var handlers = this.asyncErrHandlers;
      new Thread(() -> {
        for (var handler : handlers) {
          try {
            handler.handle(err, errOcc);
          } catch (Throwable t) {}
        }
      }).start();
    }
  }
}
