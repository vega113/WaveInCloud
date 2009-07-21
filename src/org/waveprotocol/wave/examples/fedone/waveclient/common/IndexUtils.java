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

import com.google.common.collect.Lists;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.List;

/**
 * Utilities for dealing with the index wave.
 *
 *
 */
public class IndexUtils {
  private IndexUtils() {
  }

  /** The String id of the index wave.  A WaveId also needs to contain the domain name, so we
   * can't immediately create one here. */
  public static final String INDEX_WAVE_STRING_ID = "indexwave";

  /**
   * Create a {@link WaveId} for an index wave from a given domain.
   *
   * @return index wave for that domain
   */
  public static WaveId createIndexWaveId() {
    return new WaveId("", INDEX_WAVE_STRING_ID);
  }

  /**
   * @param waveId to check
   * @return whether the waveId corresponds to an index wave
   */
  public static boolean isIndexWaveId(WaveId waveId) {
    return waveId.equals(createIndexWaveId());
  }

  /**
   * @param wave to check
   * @return whether the wave is the index wave
   */
  public static boolean isIndexWave(WaveViewData wave) {
    return isIndexWaveId(wave.getWaveId());
  }

  /**
   * Retrieve a list of wave ids from an index wave.
   *
   * @param indexWave the wave to retrieve the index from
   * @return list of wave ids
   */
  public static List<WaveId> getIndexEntries(WaveViewData indexWave) {
    List<WaveId> waveIds = Lists.newArrayList();

    if (!isIndexWave(indexWave)) {
      throw new IllegalArgumentException("Wave is not the index wave");
    }

    // The wave id is encoded as the wavelet id
    for (WaveletData wavelet : indexWave.getWavelets()) {
      waveIds.add(WaveId.deserialise(wavelet.getWaveletName().waveletId.serialise()));
    }

    return waveIds;
  }
}
