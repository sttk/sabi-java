/*
 * module-info.java
 * Copyright (C) 2022-2026 Takayuki Sato. All Rights Reserved.
 */

/**
 * Defines the module APIs of the Sabi framework.
 *
 * <p>This module provides interfaces and classes to abstract data access to external data stores,
 * manage data source and connection lifecycles, and execute logic functions in transactional or
 * non-transactional scopes.
 *
 * @version 1.0
 */
module com.github.sttk.sabi {
  exports com.github.sttk.sabi;

  requires transitive com.github.sttk.errs;
}
