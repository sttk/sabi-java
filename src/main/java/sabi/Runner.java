/*
 * Runner class.
 * Copyright (C) 2022-2023 Takayuki Sato. All Rights Reserved.
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
 * {@link Seq#run} or {@link Para#run}.
 */
public interface Runner {

  /**
   * Runs a process represented by this class.
   *
   * @throws Err  If an exception occurs in this process.
   */
  void run() throws Err;
}
