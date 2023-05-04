/*
 * Para class.
 * Copyright (C) 2023 Takayuki Sato. All Rights Reserved.
 */
package sabi;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

/**
 * Para is a class to runs {@link Runner}(s) in parallel.
 */
public class Para {

  /** The array of {@link Runner}(s). */
  private final Runner[] runners;

  /**
   * An error reason which indicates some runner which is runned in parallel
   * failed.
   *
   * @param errors  A map contains {@link Err} objects with parallelized
   *   runner's indexes.
   */
  public record FailToRunInParallel(Map<Integer, Err> errors) {};

  /**
   * An error reason which indicates that an exception occurs in parallelized
   * runner.
   */
  public record RunInParallelExceptionOccurs() {};

  /**
   * The constructor which takes an array of {@link Runner}(s) as arguments.
   *
   * @param runners  {@link Runner}'s variadic arguments.
   */
  public Para(Runner ...runners) {
    this.runners = runners;
  }

  /**
   * Runs the {@link Runner}(s) holding in this object in parallel.
   *
   * @throws Err  If it is failed to run some of the runners.
   */
  public void run() throws Err {
    Para.run(runners);
  }

  /**
   * Runs the specified {@link Runner}(s) in parallel.
   *
   * @param runners  {@link Runner}'s variadic arguments.
   * @throws Err  If it is failed to run some of the runners.
   */
  public static void run(final Runner... runners) throws Err {
    final var executors = Executors.newFixedThreadPool(runners.length);
    final var futures = new ArrayList<Future>(runners.length);
    final var errors = new HashMap<Integer, Err>();
    try {
      for (var runner : runners) {
        futures.add(executors.submit(() -> {
          runner.run();
          return null;
        }));
      }

      final var it = futures.iterator();
      for (int i = 0; it.hasNext(); i++) {
        try {
          it.next().get();
        } catch (ExecutionException exc) {
          var cause = exc.getCause();
          if (cause instanceof Err) {
            errors.put(i, Err.class.cast(cause));
          } else {
            errors.put(i, new Err(new RunInParallelExceptionOccurs(), cause));
          }
        } catch (Exception e) {
          errors.put(i, new Err(new RunInParallelExceptionOccurs(), e));
        }
      }
    } finally {
      executors.shutdown();
    }

    if (!errors.isEmpty()) {
      throw new Err(new FailToRunInParallel(errors));
    }
  }
}
