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

import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.waveserver.federation.WaveletFederationProvider;

import java.util.List;

/**
 * Remote wavelets differ from local ones in that deltas are not submitted for OT,
 * rather they are updated when a remote wave service provider has applied and sent
 * a delta.
 *
 *
 */
interface RemoteWaveletContainer extends WaveletContainer {

  interface Factory {
    /** @throws IllegalArgumentException if the waveletName is bad */
    RemoteWaveletContainer create(WaveletName waveletName);
  }

  /**
   * Update the state of the remote wavelet. This acts somewhat like a high
   * water mark - if the provided deltas would continue a contiguous block from
   * version zero, then they will be immediately transformed and returned to the
   * client. If they do not, then an asynchronous callback will be kicked off to
   * request the missing deltas.
   *
   * @param appliedDeltas the list of deltas for the update.
   * @param domain the listener domain where these deltas were receievd
   * @param federationProvider the provider where missing data may be sourced
   * @param certificateManager for verifying signatures and requesting signer info
   * @param updateCallback for asynchronous notification when deltas are ready
   *        to be processed, providing a transformed version of the applied
   *        delta and the hashed version after application
   * @throws WaveServerException
   */
  void update(List<ByteStringMessage<ProtocolAppliedWaveletDelta>> appliedDeltas, String domain,
      WaveletFederationProvider federationProvider, CertificateManager certificateManager,
      RemoteWaveletDeltaCallback updateCallback) throws WaveServerException;

  /**
   * Indicate that the remote wave server has committed the wavelet to disk.
   * @param hashedVersion version that was committed.
   * @return whether to inform clients of the commit.
   */
  boolean committed(HashedVersion hashedVersion) throws WaveletStateException;
}
