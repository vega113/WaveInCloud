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

package org.waveprotocol.box.server.waveserver;

import org.waveprotocol.box.server.frontend.WaveletSnapshotAndVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collection;

/**
 * Provides wavelet snapshots and history, and accepts delta submissions to
 * wavelets.
 */
public interface WaveletProvider {

  /**
   * Receives the result of a delta submission request.
   */
  interface SubmitRequestListener {
    /**
     * Notifies the listener that the delta was successfully applied.
     *
     * @param operationsApplied number of operations applied
     * @param hashedVersionAfterApplication wavelet hashed version after the delta
     * @param applicationTimestamp timestamp of delta application
     */
    void onSuccess(int operationsApplied, HashedVersion hashedVersionAfterApplication,
        long applicationTimestamp);

    /**
     * Notifies the listener that the delta failed to apply.
     */
    void onFailure(String errorMessage);
  }

  /**
   * Request that a given delta is submitted to the wavelet.
   *
   * @param waveletName name of wavelet.
   * @param delta to be submitted to the server.
   * @param listener callback which will return the result of the submission.
   */
  void submitRequest(WaveletName waveletName, ProtocolWaveletDelta delta,
      SubmitRequestListener listener);

  /**
   * Retrieve the wavelet history of deltas applied to the wavelet.
   *
   * @param waveletName name of wavelet.
   * @param versionStart start version (inclusive), minimum 0.
   * @param versionEnd end version (exclusive).
   * @return deltas in the range as requested, ordered by applied version.
   * @throws AccessControlException if {@code versionStart} or
   *         {@code versionEnd} are not in the wavelet history.
   * @throws WaveletStateException if the wavelet is in a state unsuitable for
   *         retrieving history.
   */
  Collection<TransformedWaveletDelta> getHistory(WaveletName waveletName,
      HashedVersion versionStart, HashedVersion versionEnd)
      throws AccessControlException, WaveletStateException;

  /**
   * Check if the specified participantId has access to the named wavelet.
   *
   * @param waveletName name of wavelet.
   * @param participantId id of participant attempting to gain access to
   *        wavelet, or null if the user isn't logged in.
   * @throws WaveletStateException if the wavelet is in a state unsuitable for
   *         checking permissions.
   * @return true if the wavelet exists and the participant is a participant on
   *         the wavelet.
   */
  boolean checkAccessPermission(WaveletName waveletName, ParticipantId participantId)
      throws WaveletStateException;

  /**
   * Request the current state of the wavelet.
   *
   * @param waveletName the name of the wavelet
   * @return the wavelet, or null if the wavelet doesn't exist
   */
  WaveletSnapshotAndVersion getSnapshot(WaveletName waveletName);
}
