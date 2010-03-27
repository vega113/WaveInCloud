// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.operation;

import org.waveprotocol.wave.model.operation.OperationException;

/**
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface NindoSink {

  /**
   * Applies a non invertible op and returns an invertible one
   * @param op
   * @return invertible version of {@code op}
   * @throws OperationException
   */
  BufferedDocOp consumeAndReturnInvertible(Nindo op) throws OperationException;

  /**
   * Version that does not throw operation exceptions.
   *
   * @author danilatos@google.com (Daniel Danilatos)
   */
  public interface Silent extends NindoSink {

    @Override
    BufferedDocOp consumeAndReturnInvertible(Nindo op);
  }
}
