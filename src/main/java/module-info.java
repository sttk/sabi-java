/*
 * module-info.
 * Copyright (C) 2022-2025 Takayuki Sato. All Rights Reserved.
 */

/**
 * Defines the APIs of Sabi framework.
 *
 * <p>This module includes the interfaces that abstracts data accesses to the external data stores
 * and the classes to execute a logic function with or without transaction operations.
 *
 * @version 0.5
 */
module com.github.sttk.sabi {
  exports com.github.sttk.sabi;

  requires transitive com.github.sttk.errs;
}
