/*
 * Err class.
 * Copyright (C) 2022 Takayuki Sato. All Rights Reserved.
 */
package sabi;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.HashMap;
import java.util.StringJoiner;


/**
 * Err is an exception class with a reason.
 * <br>
 * This class has a record value which indicates a reason by which this
 * exception is caused.
 * A record as a reason has some fields that helps to know error situation
 * where an exception of this class is caused.
 * <br>
 * The example code of creating and throwing an exception is as follows:
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

  /* The stack trace for the location of occurrence. */
  private final StackTraceElement trace;


  /**
   * A constructor which takes a reason of this exception as an argument.
   *
   * @param reason  A reason of this exception.
   */
  public Err(final Record reason) {
    if (reason == null) {
      throw new NullPointerException("reason");
    }
    this.reason = reason;

    var traces = getStackTrace();
    this.trace = traces[0];
  }


  /**
   * A constructor which takes a reason and a cause exception of this exception
   * as arguments.
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

    var traces = getStackTrace();
    this.trace = traces[0];
  }


  /**
   * Gets the reason of this exception.
   *
   * @return  The reason of this exception.
   */
  public Record getReason() {
    return this.reason;
  }


  /**
   * Gets an error situation parameter value by the specified name.
   *
   * @param name  An error situation parameter name.
   * @return  An error situation parameter value.
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
   * Gets an error situation parameter value by the specified name.
   *
   * @param name  An enum value for an error situation parameter.
   * @return  An error situation parameter value.
   */
  public Object get(final Enum<?> name) {
    return get(name.name());
  }


  /**
   * gets a map which contains error situation parameters. 
   *
   * @return  A map of error situation parameters.
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
   * Gets a message represents this exception.
   *
   * @return  A message.
   */
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
   * Returns the fully qualified name of the class of this error occurrence.
   *
   * @return  The fully qualified name of the class of this error occurrence.
   */
  public String getClassName() {
    return trace.getClassName();
  }


  /**
   * Returns the name of the method of this error occurrence.
   *
   * @return  The name of the method of this error occurrence.
   */
  public String getMethodName() {
    return trace.getMethodName();
  }


  /**
   * Returns the name of the source file of this error occurrence.
   *
   * @return  The name of the source file of this error occurrence.
   */
  public String getFileName() {
    return trace.getFileName();
  }


  /**
   * Returns the name of the source file of this error occurrence.
   *
   * @return  The name of the source file of this error occurrence.
   */
  public int getLineNumber() {
    return trace.getLineNumber();
  }
}
