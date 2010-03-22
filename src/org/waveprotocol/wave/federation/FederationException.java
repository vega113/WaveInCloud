// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.federation;

import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;

/**
 * Exception thrown by Federation components, containing Federation error codes (as per the
 * protocol) to send on failure conditions.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public class FederationException extends Exception {

  private final FederationError error;

  /**
   * @return the Federation error associated with this exception
   */
  public FederationError getError() {
    return error;
  }

  public FederationException(FederationError error, Throwable t) {
    super(t);
    this.error = error;
  }

  public FederationException(FederationError.Code errorCode) {
    this(FederationError.newBuilder().setErrorCode(errorCode).build());
  }

  public FederationException(FederationError.Code errorCode, String errorMessage) {
    this(FederationError.newBuilder()
        .setErrorCode(errorCode)
        .setErrorMessage(errorMessage).build());
  }

  public FederationException(FederationError.Code errorCode, Throwable t) {
    this(errorCode, t.getMessage(), t);
  }

  public FederationException(FederationError.Code errorCode, String errorMessage, Throwable t) {
    this(FederationError.newBuilder()
        .setErrorCode(errorCode)
        .setErrorMessage(errorMessage).build(), t);
  }

  public FederationException(FederationError error) {
    super();
    this.error = error;
  }

  @Override
  public String toString() {
    return error.hasErrorMessage()
        ? "FederationException(" + error.getErrorCode() + ": " + error.getErrorMessage() + ")"
        : "FederationException(" + error.getErrorCode() + ") ";
  }

}
