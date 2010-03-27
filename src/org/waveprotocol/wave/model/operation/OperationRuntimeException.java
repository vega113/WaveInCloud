// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.operation;


/**
 * Operation exception boxed into a runtime exception.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
@SuppressWarnings("serial")
public class OperationRuntimeException extends RuntimeException {

  private final OperationException opException;
  private final String message;

  /**
   * @param message Description of unexpected failure
   * @param opException The boxed exception
   */
  public OperationRuntimeException(String message, OperationException opException) {
    super(message, opException);
    this.message = message;
    this.opException = opException;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "BoxedOpException: " + message + ": " + opException;
  }

  /**
   * @return The boxed exception
   */
  public OperationException get() {
    return opException;
  }
}
