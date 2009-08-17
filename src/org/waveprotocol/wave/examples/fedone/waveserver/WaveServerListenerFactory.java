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

import com.google.protobuf.ByteString;

import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.protocol.common.ProtocolHashedVersion;

import java.util.List;

/**
 * Produces WaveletFederationListener instances backed onto the wave server that
 * push updates for a specific domain.
 *
 *
 */
public class WaveServerListenerFactory implements WaveletFederationListener.Factory {

  WaveServerListenerFactory() {
  }

  @Override
  public WaveletFederationListener listenerForDomain(final String domain) {
    return new WaveletFederationListener() {
      @Override
      public void waveletUpdate(WaveletName waveletName, List<ByteString> deltas,
          ProtocolHashedVersion committedVersion, WaveletUpdateCallback callback) {
        // TODO Auto-generated method stub
      }};
  }
}
