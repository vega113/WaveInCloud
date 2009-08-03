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

package org.waveprotocol.wave.examples.fedone.waveclient.common;

import com.google.common.collect.Maps;

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactory;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;

import java.util.Map;

/**
 * A client's view of a wave, with the current wavelet versions.
 *
 *
 */
public class ClientWaveView {
  /** Wave this is the view of. */
  private final WaveViewData data;

  /** Last known version of each wavelet. */
  private final Map<WaveletId, HashedVersion> currentVersions;

  /** Factory for creating hashed versions. */
  private final HashedVersionFactory hashedVersionFactory;

  /**
   * @param hashedVersionFactory for generating hashed versions
   * @param waveId of the wave
   */
  public ClientWaveView(HashedVersionFactory hashedVersionFactory, WaveId waveId) {
    this.hashedVersionFactory = hashedVersionFactory;
    this.data = new WaveViewDataImpl(waveId);
    this.currentVersions = Maps.newHashMap();
  }

  /**
   * Get the unique identifier of the wave in view.
   *
   * @return the unique identifier of the wave.
   */
  public WaveId getWaveId() {
    return data.getWaveId();
  }

  /**
   * Gets the wavelets in this wave view. The order of iteration is unspecified.
   *
   * @return wavelets in this wave view.
   */
  public Iterable<? extends WaveletData> getWavelets() {
    return data.getWavelets();
  }

  /**
   * Gets the last known version for a wavelet.
   *
   * @param waveletId of the wavelet
   * @return last known version for wavelet
   */
  public HashedVersion getWaveletVersion(WaveletId waveletId) {
    HashedVersion version = currentVersions.get(waveletId);
    if (version == null) {
      throw new IllegalArgumentException(waveletId + " is not a wavelet of " + data.getWaveId());
    } else {
      return version;
    }
  }

  /**
   * Sets the last known version for a wavelet.
   *
   * @param waveletId of the wavelet
   * @param version of the wavelet
   */
  public void setWaveletVersion(WaveletId waveletId, HashedVersion version) {
    currentVersions.put(waveletId, version);
  }

  /**
   * Get a wavelet from the view by id.
   *
   * @return the requested wavelet, or null if it is not in view.
   */
  public WaveletData getWavelet(WaveletId waveletId) {
    return data.getWavelet(waveletId);
  }

  /**
   * Create a wavelet in the wave.
   *
   * @param waveletId of new wavelet, which must be unique within the wave
   * @return wavelet created
   */
  public WaveletData createWavelet(WaveletId waveletId) {
    WaveletName name = WaveletName.of(data.getWaveId(), waveletId);
    WaveletData wavelet = data.createWavelet(waveletId);
    currentVersions.put(waveletId, hashedVersionFactory.createVersionZero(name));
    return wavelet;
  }

  /**
   * Removes a wavelet and its current hashed version from the wave view.
   *
   * @param waveletId of wavelet to remove
   */
  public void removeWavelet(WaveletId waveletId) {
    data.removeWavelet(waveletId);
    currentVersions.remove(waveletId);
  }
}
