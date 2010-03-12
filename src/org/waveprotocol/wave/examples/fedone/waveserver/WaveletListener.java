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
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.id.WaveletName;

import java.util.List;
import java.util.Map;

/**
 * Interface that's used by the WaveView classes to listen to updates on
 * wavelets.
 *
 *
 */
public interface WaveletListener {

  /**
   * This message is passed when a wavelet is updated, the receiver can
   * calculate the current version of the wavelet by summing the version
   * in delta.hashedVersion with the count of operations in the delta.
   *
   * @param waveletName name of the wavelet.
   * @param newDeltas applied to the wavelet, containing transformed operations
   * @param resultingVersion of the wavelet after applying the deltas
   * @param documentState the state of each document (by documentId) in the
   *                      wavelet after the deltas have been applied.
   */
  void waveletUpdate(WaveletName waveletName, List<ProtocolWaveletDelta> newDeltas,
      ProtocolHashedVersion resultingVersion, Map<String, BufferedDocOp> documentState);

  /**
   * Called when a wavelet is committed to disk at a specific version. This notifies
   * the listener that the hosting provider has reliably committed the wavelet and any
   * state pertaining to the committed data held by listeners may be released.
   *
   * @param waveletName name of wavelet.
   * @param version the version and hash of the wavelet as it was committed by
   *                the hosting provider.
   */
  void waveletCommitted(WaveletName waveletName, ProtocolHashedVersion version);
}
