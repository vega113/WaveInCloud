// Copyright 2009 Google Inc. All Rights Reserved.
package org.waveprotocol.wave.model.document.indexed;

import org.waveprotocol.wave.model.document.operation.Nindo;

import org.waveprotocol.wave.model.operation.OperationException;

/**
 * A Nindo Validator associated with an IndexedDocument and schema.
 *
*
 */
public interface Validator {

  /**
   * Validates the nindo in this context.
   *
   * @throws OperationException if validation fails.
   */
  void maybeThrowOperationExceptionFor(Nindo op) throws OperationException;
}
