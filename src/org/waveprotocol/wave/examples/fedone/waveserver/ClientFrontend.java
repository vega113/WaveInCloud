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

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.protocol.common.ProtocolHashedVersion;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

import java.util.List;
import java.util.Set;


/**
 * The client frontend handles requests from the client, and updates from the
 * waveserver.
 *
 * It receives updates for all wavelets from the waveserver.
 *
 * Sends updates for those wavelets that a client has "opened" (matching waveIds
 * optionally wavelet prefix match, only for wavelets that the participant has access to).
 * When a wavelet is added and it's not at version 0, buffer updates until a
 * request for the wavelet's history has completed.
 *
 *
 *
 */
public interface ClientFrontend extends WaveletListener {

  /**
   * Request submission of a delta.
   *
   * @param waveletName name of wavelet.
   * @param delta the wavelet delta to submit.
   * @param listener callback for the result.
   */
  void submitRequest(WaveletName waveletName, ProtocolWaveletDelta delta,
      SubmitResultListener listener);

  interface SubmitResultListener {
    void onSuccess(int operationsApplied);
    void onFailure(String errorMessage);
  }

  /**
   * Request to open a Wave. Optional waveletIdPrefixes allows the requestor to
   * constrain which wavelets to include in the updates.
   *
   * @param participant which is doing the requesting.
   * @param waveId the wave id.
   * @param waveletIdPrefixes set containing restricts on the wavelet id's.
   * @param maximumInitialWavelets limit on the number of wavelets to
   * @param openListener callback for updates.
   */

  void openRequest(ParticipantId participant, WaveId waveId, Set<String> waveletIdPrefixes,
      int maximumInitialWavelets, OpenListener openListener);

  interface OpenListener {
    void onUpdate(WaveletName waveletName, List<ProtocolWaveletDelta> deltas,
        ProtocolHashedVersion endVersion);
    void onCommit(WaveletName waveletName, ProtocolHashedVersion commitNotice);
    void onFailure(String errorMessage);
  }
}
