/**
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.waveprotocol.wave.examples.fedone.util;

/**
 * Indicates a submitted delta contained no operations. This should not a fatal
 * error (for example, OT could transform a non-empty delta to an empty one),
 * although hosts should not federate empty deltas -- so for sanity checking
 * remote servers should pay attention.
 */
public class EmptyDeltaException extends Exception {
  public EmptyDeltaException() {
    super();
  }

  public EmptyDeltaException(String message) {
    super(message);
  }

  public EmptyDeltaException(String message, Throwable cause) {
    super(message, cause);
  }

  public EmptyDeltaException(Throwable cause) {
    super(cause);
  }
}
