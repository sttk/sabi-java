/*
 * Err class.
 * Copyright (C) 2022-2023 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi.errs;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;
import java.util.StringJoiner;

import com.github.sttk.sabi.errs.notify.ErrNotifier;

/**
 * {@code Err} is the exception class with a reason.
 * <br>
 * This class has a record value which indicates a reason by which this
 * exception is caused.
 * A record as a reason can have some fields that helps to know error situation
 * where this exception is caused.
 * <br>
 * The example code of creating and throwing an excepton is as follows:
 * <pre>{@code
 *   public record FailToDoSomething(String name, int value) {}
 *
 *   throw new Err(new FailToDoSomething("abc", 123));
 * }</pre>
 */
public final class Err extends Exception {

  /** The serial version UID. */
  private static final long serialVersionUID = -4983633951387450536L;

  /** The reason by which this exception is caused. */
  private final Record reason;

  /** The stack trace for the location of occurrence. */
  private final StackTraceElement trace;

  /** The notifier of {@link Err} instance creations. */
  private static final ErrNotifier notifier = new ErrNotifier();

  /**
   * A constructor which constructs a new {@link Err} instance with a specified
   * reason.
   * A reason is a structore type of which name expresses what is a reason.
   *
   * @param reason  A reason of this exception.
   */
  public Err(final Record reason) {
    if (reason == null) {
      throw new NullPointerException("reason");
    }
    this.reason = reason;

    this.trace = getStackTrace()[0];

    notifier.notify(this);
  }

  /**
   * A constructor which constructs a new {@link Err} instance with a specified
   * reason and an cause.
   * A reason is a structore type of which name expresses what is a reason.
   *
   * @param reason  A reason of this exception.
   * @param cause  A cause exception.
   */
  public Err(final Record reason, final Throwable cause) {
    super(cause);

    if (reason == null) {
      throw new NullPointerException("reason");
    }
    this.reason = reason;

    this.trace = getStackTrace()[0];
  }

  /**
   * Gets the reason's {@link Record} instance of this exception.
   *
   * @return  The reason of this exception.
   */
  public Record getReason() {
    return this.reason;
  }

  /**
   * Gets the simple name of this reason's class.
   *
   * @return  The simple name of this reason's class.
   */
  public String getReasonName() {
    return this.reason.getClass().getSimpleName();
  }

  /**
   * Gets the package full name of this reason's class.
   *
   * @return  The package full name of this reason's class.
   */
  public String getReasonPackage() {
    return this.reason.getClass().getPackageName();
  }

  /**
   * Gets a message expresses the content of this exception.
   *
   * @return  A error message.
   */
  @Override
  public String getMessage() {
    final var sj = new StringJoiner(", ", "{", "}");

    sj.add("reason=" + this.reason.getClass().getSimpleName());

    for (Field f : this.reason.getClass().getDeclaredFields()) {
      try {
        f.setAccessible(true);
        sj.add(f.getName() + "=" + f.get(this.reason));
      } catch (Exception e) {}
    }

    final Throwable t = getCause();
    if (t != null) {
      if (t instanceof Err) {
        sj.add("cause=" + Err.class.cast(t).getMessage());
      } else {
        sj.add("cause=" + t.toString());
      }
    }

    return sj.toString();
  }

  /**
   * Gets a field value of the reason object by the specified name.
   * If the specified named field is not found in the reason of this {@link Err},
   * this method finds a same named field in reasons of cause exception
   * hierarchically.
   *
   * @param name  A field name of the reason object in this exception.
   * @return  A field value of the reason object by the specified name.
   */
  public Object get(final String name) {
    try {
      final Field f = this.reason.getClass().getDeclaredField(name);
      f.setAccessible(true);
      return f.get(this.reason);
    } catch (NoSuchFieldException e) {
      final Throwable t = getCause();
      if (t != null && t instanceof Err) {
        return Err.class.cast(t).get(name);
      }
    } catch (Exception e) {}

    return null;
  }

  /**
   * Gets a field value of the reason object by the specified {@link Enum} 
   * name.
   * If the specified named field is not found in the reason of this {@link Err},
   * this method finds a same named field in reasons of cause exception
   * hierarchically.
   *
   * @param name  An {@link Enum} that same with a field name of the reason
   *   object in this exception.
   * @return  A field value of the reason object by the specified {@link Enum}
   *   name.
   */
  public Object get(final Enum<?> name) {
    return get(name.name());
  }

  /**
   * Gets a map which contains variables that represent error situation.
   *
   * @return  A map of error situation variables.
   */
  public Map<String, Object> getSituation() {
    final var map = new HashMap<String, Object>();
    for (Field f : this.reason.getClass().getDeclaredFields()) {
      try {
        f.setAccessible(true);
        map.put(f.getName(), f.get(this.reason));
      } catch (Exception e) {}
    }

    final Throwable t = getCause();
    if (t != null && t instanceof Err) {
      map.putAll(Err.class.cast(t).getSituation());
    }

    return map;
  }

  /**
   * Gets the name of the source file of this error occurance.
   *
   * This method can return null if this information is unavailable.
   *
   * @return  The name of the source file of this error occurence.
   */
  protected String getFileName() {
    return trace.getFileName();
  }

  /**
   * Gets the line number in the source file of this error occurence.
   *
   * This method can return a negative number if this information is
   * unavailable.
   *
   * @return  The line number in the source file of this error occurence.
   */
  protected int getLineNumber() {
    return trace.getLineNumber();
  }

  /**
   * Adds an {@link ErrHandler} object which is executed synchronously just
   * after an {@link Err} is created.
   *
   * Handlers added with this method are executed in the order of addition
   * and stop if one of the handlers throws a {@link RuntimeException} or
   * an {@link Error}.
   *
   * @param handler  An {@link ErrHandler} object.
   */
  public static void addSyncHandler(final ErrHandler handler) {
    notifier.addSyncHandler(handler);
  }

  /**
   * Adds an {@link ErrHandler} object which is executed asynchronously just
   * after an {@link Err} is created.
   *
   * Handlers don't stop even if one of the handlers throw a
   * {@link RuntimeException} or an {@link Error}.
   *
   * @param handler  An {@link ErrHandler} object.
   */
  public static void addAsyncHandler(final ErrHandler handler) {
    notifier.addAsyncHandler(handler);
  }

  /**
   * Fixes configuration of {@link Err}.
   *
   * After calling this method, any more {@link ErrHandler}s cannot be
   * registered, and notification of {@link Err} creations becomes effective.
   */
  public static void fixCfg() {
    notifier.fix();
  }
}
