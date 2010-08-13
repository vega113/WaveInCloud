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

package org.waveprotocol.wave.examples.fedone.frontend;

import com.google.inject.internal.Nullable;

import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletListener;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.waveserver.federation.SubmitResultListener;

import java.util.List;
import java.util.Set;


/**
 * The client front-end handles requests from clients and directs them to
 * appropriate back-ends.
 * 
 * Provides updates for wavelets that a client has opened and access to.
 */
public interface ClientFrontend extends WaveletListener {

  /**
   * Listener provided to open requests.
   */
  interface OpenListener {
    /**
     * Called when an update is received.
     */
    void onUpdate(WaveletName waveletName, @Nullable WaveletSnapshotAndVersions snapshot,
        List<ProtocolWaveletDelta> deltas, @Nullable ProtocolHashedVersion endVersion,
        @Nullable ProtocolHashedVersion committedVersion, final boolean hasMarker,
        final String channel_id);

    /**
     * Called when the stream fails. No further updates will be received.
     */
    void onFailure(String errorMessage);
  }
  
  /**
   * Request submission of a delta.
   *
   * @param waveletName name of wavelet.
   * @param delta the wavelet delta to submit.
   * @param channelId the client's channel ID
   * @param listener callback for the result.
   */
  void submitRequest(WaveletName waveletName, ProtocolWaveletDelta delta, String channelId,
      SubmitResultListener listener);

  /**
   * Request to open a Wave. Optional waveletIdPrefixes allows the requestor to
   * constrain which wavelets to include in the updates.
   *
   * @param participant which is doing the requesting.
   * @param waveId the wave id.
   * @param waveletIdPrefixes set containing restricts on the wavelet id's.
   * @param maximumInitialWavelets limit on the number of wavelets to
   * @param snapshotsEnabled true if the client understands snapshots
   * @param knownWavelets a list of (waveletid, waveletversion pairs).
   *                      the server will send deltas to update the client to current.
   * @param openListener callback for updates.
   */

  void openRequest(ParticipantId participant, WaveId waveId, Set<String> waveletIdPrefixes,
      int maximumInitialWavelets, boolean snapshotsEnabled,
      final List<WaveClientRpc.WaveletVersion> knownWavelets, OpenListener openListener);
}
