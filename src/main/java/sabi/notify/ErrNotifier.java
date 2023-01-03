/*
 * ErrNotifier class.
 * Copyright (C) 2022 Takayuki Sato. All Rights Reserved.
 */
package sabi.notify;

import sabi.Err;
import sabi.ErrHandler;

import java.util.List;
import java.util.LinkedList;
import java.time.OffsetDateTime;

/**
 * This class notifies {@link Err} object creations to {@link ErrHandler}.
 * This class manages a list for handlers which is processed synchronously and
 * another list for handlers which is processed asynchronously.
 *
 * The only one instance of this class is created as a static field of {@link
 * Err} class.
 */
public final class ErrNotifier {

  /** The flag meaning whether this instance is sealed or not. */
  private boolean isSealed = false;

  /**
   * The list which holds {@link ErrHandler} objects which is executed
   * synchronously.
   */
  protected final List<ErrHandler> syncHandlers = new LinkedList<>();


  /**
   * The list which holds {@link ErrHandler} objects which is executed
   * asynchronously.
   */
  protected final List<ErrHandler> asyncHandlers = new LinkedList<>();


  /**
   * Constructs an instance of this class with no argument.
   */
  public ErrNotifier() {}


  /**
   * Checks whether this object is sealed or not.
   */
  public boolean isSealed() {
    return this.isSealed;
  }


  /**
   * Registers an {@link ErrHandler} object which is processed synchronously.
   * After calling {@link #seal}, this method registers no more.
   *
   * @param handler  An {@link ErrHandler} object.
   */
  public synchronized void addSyncHandler(final ErrHandler handler) {
    if (this.isSealed) {
      return;
    }
    this.syncHandlers.add(handler);
  }


  /**
   * Registers an {@link ErrHandler} object which is processed asynchronously.
   * After calling {@link #seal}, this method registers no more.
   *
   * @param handler  An {@link ErrHandler} object.
   */
  public synchronized void addAsyncHandler(final ErrHandler handler) {
    if (this.isSealed) {
      return;
    }
    this.asyncHandlers.add(handler);
  }


  /**
   * Seals this object.
   * This method makes it impossible to add more {@link ErrHandler}s to this
   * object, and possible to notify that an {@link Err} object is created.
   */
  public synchronized void seal() {
    this.isSealed = true;
  }


  /**
   * Notifies that an {@link Err} object is created.
   * However, this method does nothing until this object is sealed.
   */
  public void notify(final Err err) {
    if (!this.isSealed) {
      return;
    }

    final var now = OffsetDateTime.now();

    for (var handler : this.syncHandlers) {
      handler.handle(err, now);
    }

    if (!this.asyncHandlers.isEmpty()) {
      final var handlers = this.asyncHandlers;
      new Thread(() -> {
        for (var handler : handlers) {
          try {
            handler.handle(err, now);
          } catch (Throwable t) {}
        }
      }).start();
    }
  }
}
