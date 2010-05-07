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

import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.waveserver.SubmitResultListener;

import java.util.NavigableSet;

/**
 * Interface that's used by the WaveView classes to access the waveserver.
 *
 *
 */
public interface WaveletProvider {
  /**
   * Sets the wavelet listener for this provider.
   *
   * @param listener the wavelet listener to use for this provider.
   */
  void setListener(WaveletListener listener);

  /**
   * Request that a given delta is submitted to the wavelet.
   *
   * @param waveletName name of wavelet.
   * @param delta to be submitted to the server.
   * @param listener callback which will return the result of the submission.
   */
  void submitRequest(WaveletName waveletName, ProtocolWaveletDelta delta,
      SubmitResultListener listener);

  /**
   * Retrieve the wavelet history of deltas applied to the wavelet.
   *
   * @param waveletName name of wavelet.
   * @param versionStart start version (inclusive), minimum 0.
   * @param versionEnd end version (exclusive).
   * @return deltas in the range as requested, or null if there was an error. Note that
   *         if a delta straddles one of the requested version boundaries, it will be included.
   */
  NavigableSet<ProtocolWaveletDelta> getHistory(WaveletName waveletName,
      ProtocolHashedVersion versionStart, ProtocolHashedVersion versionEnd);

  /**
   * Request the current state of the wavelet.
   *
   * @param waveletName the name of the wavelet
   * @return the wavelet as @code{WaveletData} or null if the wavelet doesn't exist
   */
  <T> T getSnapshot(WaveletName waveletName, WaveletSnapshotBuilder<T> builder);

  // TODO(arb): add getLastCommittedVersion
}
