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

package org.waveprotocol.wave.model.wave.data.impl;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.data.WaveViewData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A skeleton implementation of {@link WaveViewData}.
 *
 *
 *
 */
public class WaveViewDataImpl implements WaveViewData {

  /** The wave id */
  private final WaveId id;

  /** Wavelets in this wave. */
  private final Map<WaveletId, WaveletDataImpl> wavelets;

  /**
   * Creates a new wave.
   *
   * @param id the wave id.
   */
  public WaveViewDataImpl(WaveId id) {
    this.id = id;
    this.wavelets = new HashMap<WaveletId, WaveletDataImpl>();
  }

  @Override
  public WaveId getWaveId() {
    return id;
  }

  @Override
  public Iterable<? extends WaveletDataImpl> getWavelets() {
    return Collections.unmodifiableCollection(wavelets.values());
  }

  @Override
  public WaveletDataImpl getWavelet(WaveletId waveletId) {
    return wavelets.get(waveletId);
  }

  @Override
  public WaveletDataImpl createWavelet(WaveletId waveletId) {
    if (wavelets.containsKey(waveletId)) {
      throw new IllegalArgumentException("Duplicate wavelet id: " + waveletId);
    }
    WaveletDataImpl newWavelet = new WaveletDataImpl(id, waveletId);
    wavelets.put(waveletId, newWavelet);
    return newWavelet;
  }

  @Override
  public void removeWavelet(WaveletId waveletId) {
    if (wavelets.remove(waveletId) == null) {
      throw new IllegalArgumentException(waveletId + " is not present");
    }
  }
}
