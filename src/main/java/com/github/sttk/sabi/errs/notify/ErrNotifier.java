/*
 * ErrNotifier class.
 * Copyright (C) 2022-2023 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.errs.notify;

import com.github.sttk.sabi.errs.Err;
import com.github.sttk.sabi.errs.ErrOcc;
import com.github.sttk.sabi.errs.ErrHandler;

import java.util.List;
import java.util.LinkedList;

/**
 * ErrNotifier is the class that notifies {@link Err} creations to {@link
 * ErrHandler}(s).
 * This class manages a list for handlers that process a {@link Err}
 * synchronously and another list for handlers that process it asynchronously.
 *
 * The only one instance of this class is created as a static field of {@link
 * Err} class.
 */
public final class ErrNotifier {

  /** The flag that indicates whether this instance is fixed or not. */
  private boolean isFixed = false;

  /**
   * The list that holds {@link ErrHandler} objects which is executed
   * synchronously.
   */
  protected final List<ErrHandler> syncErrHandlers = new LinkedList<>();

  /**
   * The list that holds {@link ErrHandler} objects which is executed
   * asynchronously.
   */
  protected final List<ErrHandler> asyncErrHandlers = new LinkedList<>();

  /**
   * Constructs an instance of this class with no argument.
   */
  public ErrNotifier() {}

  /**
   * Checks whether this object is fixed or not.
   *
   * @return  [@code true} if this object is fixed.
   */
  public boolean isFixed() {
    return this.isFixed;
  }

  /**
   * Adds an {@link ErrHandler} object that is processed synchronously.
   * After calling {@link #fix}, this method adds no more.
   *
   * @param handler  An {@link ErrHandler} object.
   */
  public void addSyncHandler(final ErrHandler handler) {
    if (this.isFixed) {
      return;
    }
    this.syncErrHandlers.add(handler);
  }

  /**
   * Adds an {@link ErrHandler} object that is processed asynchronously.
   * After calling {@link #fix}, this method adds no more.
   *
   * @param handler  An {@link ErrHandler} object.
   */
  public void addAsyncHandler(final ErrHandler handler) {
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
  public void fix() {
    this.isFixed = true;
  }

  /**
   * Notifies that an {@link Err} object is created.
   * However, this method does nothging until this object is fixed.
   *
   * @param err  An {@link Err} object.
   */
  public void notify(final Err err) {
    if (!this.isFixed) {
      return;
    }

    final var occ = new ErrOcc(err);

    for (var handler : this.syncErrHandlers) {
      handler.handle(err, occ);
    }

    if (!this.asyncErrHandlers.isEmpty()) {
      for (var handler : this.asyncErrHandlers) {
        Thread.ofVirtual().start(() -> {
          handler.handle(err, occ);
        });
      }
    }
  }
}
