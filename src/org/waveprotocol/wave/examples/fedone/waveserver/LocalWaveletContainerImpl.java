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

import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.protocol.common.ProtocolSignedDelta;

/**
 * A local wavelet may be updated by submits. The local wavelet will perform
 * operational transformation on the submitted delta and assign it the latest
 * version of the wavelet.
 *
 *
 *
 */

class LocalWaveletContainerImpl extends WaveletContainerImpl
    implements LocalWaveletContainer {

  public LocalWaveletContainerImpl(WaveletName waveletName) {
    super(waveletName);
  }

  @Override
  public DeltaApplicationResult submitRequest(WaveletName waveletName,
      ProtocolSignedDelta signedDelta) throws OperationException,
      AccessControlException {

    acquireWriteLock();
    try {
      // Pass through the current system time, as this is a locally hosted
      // wavelet (we decide when it is applied).
      return transformAndApplyDelta(signedDelta, System.currentTimeMillis());
    } finally {
      releaseWriteLock();
    }
  }
}
