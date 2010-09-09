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

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.util.WaveletDataUtil;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletProvider;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletSnapshotBuilder;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.waveserver.federation.SubmitResultListener;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 * Stub of {@link WaveletProvider} for testing. It only supports getSnapshot().
 * 
 * It currently hosts a single wavelet, which contains a single document.
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class WaveletProviderStub implements WaveletProvider {
  private WaveletData wavelet;
  private HashedVersion currentVersionOverride;
  private ProtocolHashedVersion committedVersion;
  
  public WaveletProviderStub() {
    wavelet = createSimpleWaveletData();

    // This will be null in practice until the persistence store is in place.
    setCommittedVersion(null);
  }

  /**
   * Create a wavelet data object for testing. Do not depend on the wavelet
   * returned by this method to remain stable.
   * 
   * @return a simple wavelet.
   */
  public static WaveletData createSimpleWaveletData() {
    WaveletName name = WaveletName.of(new WaveId("example.com", "w+abc123"),
        new WaveletId("example.com", "conv+root"));
    ParticipantId creator;
    long time = new Date().getTime();
    
    try {
      creator = ParticipantId.of("sam@example.com");
    } catch (InvalidParticipantAddress e) {
      throw new RuntimeException(e);
    }
        
    WaveletData wavelet = WaveletDataUtil.createEmptyWavelet(name, creator, time);

    DocInitialization content = new DocInitializationBuilder().characters("Hello there").build();
    wavelet.createBlip("b+abc123", creator, Collections.<ParticipantId>emptySet(), content, time, 0);
    
    return wavelet;
  }
  
  @Override
  public <T> T getSnapshot(WaveletName waveletName, WaveletSnapshotBuilder<T> builder) {
    if (waveletName.waveId.equals(getHostedWavelet().getWaveId())
        && waveletName.waveletId.equals(getHostedWavelet().getWaveletId())) {
      HashedVersion version = currentVersionOverride != null ? currentVersionOverride
          : new HashedVersion(getHostedWavelet().getVersion(), new byte[]{0,1,2,3,4,5,-128,127});

      return builder.build(getHostedWavelet(), version, getCommittedVersion());
    } else {
      return null;
    }
  }

  @Override
  public Collection<ProtocolWaveletDelta> getHistory(WaveletName waveletName,
      ProtocolHashedVersion versionStart, ProtocolHashedVersion versionEnd) {
    throw new AssertionError("Not implemented");
  }

  @Override
  public void submitRequest(
      WaveletName waveletName, ProtocolWaveletDelta delta, SubmitResultListener listener) {
    throw new AssertionError("Not implemented");
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
