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

package org.waveprotocol.wave.examples.fedone.rpc;

import org.waveprotocol.wave.examples.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.SnapshotSerializer;
import org.waveprotocol.wave.examples.fedone.frontend.WaveletSnapshotAndVersion;
import org.waveprotocol.wave.examples.fedone.util.TestDataUtil;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletProvider;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.waveserver.federation.SubmitResultListener;

import java.util.Collection;

/**
 * Stub of {@link WaveletProvider} for testing. It only supports getSnapshot().
 *
 * It currently hosts a single wavelet, which contains a single document.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class WaveletProviderStub implements WaveletProvider {
  private final WaveletData wavelet;
  private HashedVersion currentVersionOverride;
  private ProtocolHashedVersion committedVersion;

  public WaveletProviderStub() {
    wavelet = TestDataUtil.createSimpleWaveletData();

    // This will be null in practice until the persistence store is in place.
    setCommittedVersion(null);
  }

  @Override
  public WaveletSnapshotAndVersion getSnapshot(WaveletName waveletName) {
    final byte[] JUNK_BYTES = new byte[]{0,1,2,3,4,5,-128,127};

    if (waveletName.waveId.equals(getHostedWavelet().getWaveId())
        && waveletName.waveletId.equals(getHostedWavelet().getWaveletId())) {
      HashedVersion version = (currentVersionOverride != null)
          ? currentVersionOverride
          : new HashedVersion(getHostedWavelet().getVersion(), JUNK_BYTES);
      return new WaveletSnapshotAndVersion(SnapshotSerializer.serializeWavelet(getHostedWavelet(),
          version), getCommittedVersion());
    } else {
      return null;
    }
  }

  @Override
  public Collection<ProtocolWaveletDelta> getHistory(WaveletName waveletName,
      ProtocolHashedVersion versionStart, ProtocolHashedVersion versionEnd) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void submitRequest(
      WaveletName waveletName, ProtocolWaveletDelta delta, SubmitResultListener listener) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return the wavelet
   */
  public WaveletData getHostedWavelet() {
    return wavelet;
  }

  /**
   * @param currentVersionOverride the currentVersionOverride to set
   */
  public void setVersionOverride(HashedVersion currentVersionOverride) {
    this.currentVersionOverride = currentVersionOverride;
  }

  /**
   * @param committedVersion the committedVersion to set
   */
  public void setCommittedVersion(ProtocolHashedVersion committedVersion) {
    this.committedVersion = committedVersion;
  }

  /**
   * @return the committedVersion
   */
  public ProtocolHashedVersion getCommittedVersion() {
    return committedVersion;
  }
}
