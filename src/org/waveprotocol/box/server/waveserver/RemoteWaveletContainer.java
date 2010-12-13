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

import com.google.protobuf.ByteString;

import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.federation.WaveletFederationProvider;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.List;

/**
 * Remote wavelets differ from local ones in that deltas are not submitted for OT,
 * rather they are updated when a remote wave service provider has applied and sent
 * a delta.
 */
interface RemoteWaveletContainer extends WaveletContainer {

  interface Factory {
    /**
     * @throws PersistenceException if storage access goes wrong
     * @throws IllegalArgumentException if the waveletName is bad
     */
    RemoteWaveletContainer create(WaveletName waveletName) throws PersistenceException;
  }

  /**
   * Update the state of the remote wavelet. This acts somewhat like a high
   * water mark - if the provided deltas would continue a contiguous block from
   * version zero, then they will be immediately transformed and returned to the
   * client. If they do not, then an asynchronous callback will be kicked off to
   * request the missing deltas.
   *
   * @param deltas the list of (serialized applied) deltas for the update
   * @param domain the listener domain where these deltas were received
   * @param federationProvider the provider where missing data may be sourced
   * @param certificateManager for verifying signatures and requesting signer info
   * @param updateCallback for asynchronous notification when deltas are ready
   *        to be processed, providing a transformed version of the applied
   *        delta and the hashed version after application
   */
  void update(List<ByteString> deltas, String domain,
      WaveletFederationProvider federationProvider, CertificateManager certificateManager,
      RemoteWaveletDeltaCallback updateCallback);
}
