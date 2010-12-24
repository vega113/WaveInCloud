/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.wave.federation;

import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;

/**
 * Wraps a federation error.
 *
 * @author soren@google.com (Soren Lassen)
 */
public class FederationException extends Exception {

  private final FederationError error;

  public FederationException(FederationError error, Throwable cause) {
    super(error.getErrorCode() + " " + error.getErrorMessage(), cause);
    this.error = error;
  }

  public FederationException(FederationError error) {
    this(error, null);
  }

  public FederationException(Throwable cause) {
    this(FederationErrors.internalServerError(describe(cause)), cause);
  }

  public FederationError getError() {
    return error;
  }

  private static String describe(Throwable cause) {
    return (cause instanceof InterruptedException) ? "Interrupted" : "Internal failure";
  }
}
