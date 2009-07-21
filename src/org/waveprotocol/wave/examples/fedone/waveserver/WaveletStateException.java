/**
 * Copyright 2009 Google Inc.
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

package org.waveprotocol.wave.examples.fedone.waveserver;

import org.waveprotocol.wave.examples.fedone.waveserver.WaveletContainer.State;

/**
 * Exception thrown when an attempt is made to access Wavelet content where
 * it's state is not OK.
 *
 *
 *
 */
public class WaveletStateException extends WaveServerException {
  private final State state;

  public WaveletStateException(State state, String message) {
    super(message);
    this.state = state;
  }

  public State getState() {
    return state;
  }
}
