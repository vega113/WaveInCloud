// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.federation;

import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;

/**
 * Convenience methods for creating {@code FederationError}s.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public class FederationErrors {

  private FederationErrors() {
  }

  public static FederationError newFederationError(FederationError.Code errorCode) {
    return FederationError.newBuilder().setErrorCode(errorCode).build();
  }

  public static FederationError newFederationError(FederationError.Code errorCode,
      String errorMessage) {
    return FederationError.newBuilder()
        .setErrorCode(errorCode)
        .setErrorMessage(errorMessage).build();
  }

  public static FederationError badRequest(String errorMessage) {
    return newFederationError(FederationError.Code.BAD_REQUEST, errorMessage);
  }


}
