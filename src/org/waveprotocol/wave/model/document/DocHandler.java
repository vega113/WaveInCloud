// Copyright 2010 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.document.indexed.DocumentHandler;

/**
 * A DocumentHandler specialized for the non-generic Document interface.
 *
*
 */
public interface DocHandler extends DocumentHandler<Doc.N, Doc.E, Doc.T> {

  /** Convenience interface for referring to a non-generic EventBundle. */
  public interface DocEventBundle extends DocumentHandler.EventBundle<Doc.N, Doc.E, Doc.T> { }
}
