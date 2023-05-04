/*
 * Seq class.
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
 * Seq is a class to runs {@link Runner}(s) sequencially.
 */
public class Seq implements Runner {

  /** The array of {@link Runner}(s). */
  private final Runner[] runners;

  /**
   * The constructor which takes an array of {@link Runner}(s) as arguments.
   *
   * @param runners  {@link Runner}'s variadic arguments.
   */
  public Seq(Runner ...runners) {
    this.runners = runners;
  }

  /**
   * Runs the {@link Runner}(s) holding in this object sequencially.
   *
   * @throws Err  If it is failed to run one of the runners.
   */
  @Override
  public void run() throws Err {
    run(runners);
  }

  /**
   * Runs the specified {@link Runner}(s) sequencially.
   *
   * @param runners  {@link Runner}'s variadic arguments.
   * @throws Err  If it is failed to run one of the runners.
   */
  public static void run(final Runner... runners) throws Err {
    for (var runner : runners) {
      runner.run();
    }
  }
}
