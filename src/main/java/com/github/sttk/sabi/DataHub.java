// Copyright (C) 2024-2026 Takayuki Sato. All Rights Reserved.
// This program is free software under MIT License.
// See the file LICENSE in this distribution for more details.

package com.github.sttk.sabi;

import com.github.sttk.errs.Err;
import java.util.List;

public class DataHub implements DataAcc, AutoCloseable {

  final DataRun run;

  public DataHub() {
    this.run = new DataRun();
  }

  public DataHub(String... names) {
    this.run = new DataRun(List.of(names));
  }

  @Override
  public void close() {
    this.run.close();
  }

  @Override
  public final <C extends DataConn> C getDataConn(String name, Class<C> cls) throws Err {
    return this.run.getDataConn(name, cls);
  }

  @SuppressWarnings("unchecked")
  @Override
  public final <D extends DataAcc> void run(Logic<D> logic) throws Err {
    logic.run((D) this);
  }

  @Override
  public final LogicRun start() {
    return new LogicRun(this, true);
  }
}
