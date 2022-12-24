/*
 * Runner class.
 * Copyright (C) 2022 Takayuki Sato. All Rights Reserved.
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
 * Runner is an interface which has {@link #run} method and is runned by
 * #runSeq or #runPara method.
 */
public interface Runner {

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
   * Executes a process represented by this class.
   *
   * @throws Err  If an exception occurs in this process.
   */
  void run() throws Err;

  /**
   * Runs specified runners sequencially.
   *
   * @param runners  {@link Runner}'s variadic arguments.
   */
  public static void runSeq(final Runner... runners) throws Err {
    for (var runner : runners) {
      runner.run();
    }
  }

  /**
   * Runs specified runners in parallel.
   *
   * @param runners  {@link Runner}'s variadic arguments.
   */
  public static void runPara(final Runner... runners) throws Err {
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

  /**
   * Creates a runner which runs multiple runners specified as arguments
   * sequencially.
   *
   * @param runners  {@link Runner} objects.
   */
  static Runner seq(Runner ...runners) {
    return new Runner() {
      @Override
      public void run() throws Err {
        Runner.runSeq(runners);
      }
    };
  }

  /**
   * Creates a runner which runs multiple runners specified as arguments
   * in parallel.
   *
   * @param runners  {@link Runner} objects.
   */
  static Runner para(Runner ...runners) {
    return new Runner() {
      @Override
      public void run() throws Err {
        Runner.runPara(runners);
      }
    };
  }
}
