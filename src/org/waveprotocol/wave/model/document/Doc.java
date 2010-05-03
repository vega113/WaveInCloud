// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

/**
 * Concrete vacuous interfaces for the N,E,T type
 * parameters to Document
 *
 * @see Document
 */
public final class Doc {

  // Do not add methods to these interfaces

  /** Node type */
  public interface N {}

  /** Element type */
  public interface E extends N {}

  /** Text node type */
  public interface T extends N {}

  private Doc() {}
}
