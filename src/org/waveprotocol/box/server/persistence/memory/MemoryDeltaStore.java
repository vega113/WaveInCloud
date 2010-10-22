/**
 * Copyright 2010 Google Inc.
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

package org.waveprotocol.box.server.persistence.memory;

import com.google.gxp.com.google.common.collect.ImmutableSet;

import org.waveprotocol.box.server.waveserver.DeltaStore;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Set;

/**
 * A simple in-memory implementation of DeltaStore.
 *
 * @author josephg@google.com (Joseph Gentle)
 */
public class MemoryDeltaStore implements DeltaStore {
  final Map<WaveId, Map<WaveletId, MemoryDeltaCollection>> data = CollectionUtils.newHashMap();

  private Map<WaveletId, MemoryDeltaCollection> getOrCreateWaveData(WaveId waveId) {
    Map<WaveletId, MemoryDeltaCollection> waveData = data.get(waveId);
    if (waveData == null) {
      waveData = CollectionUtils.newHashMap();
      data.put(waveId, waveData);
    }

    return waveData;
  }

  @Override
  public void delete(WaveletName waveletName) throws FileNotFoundException {
    Map<WaveletId, MemoryDeltaCollection> waveData = data.get(waveletName.waveId);
    if (waveData == null) {
      throw new FileNotFoundException();
    } else {
      if (waveData.remove(waveletName.waveletId) == null) {
        // Nothing was removed.
        throw new FileNotFoundException();
      }
    }
  }

  @Override
  public Set<WaveletId> lookup(WaveId waveId) {
    Map<WaveletId, MemoryDeltaCollection> waveData = data.get(waveId);
    if (waveData == null) {
      return ImmutableSet.of();
    } else {
      Set<WaveletId> wavelets = CollectionUtils.newHashSet();
      for (MemoryDeltaCollection collection : waveData.values()) {
        if (!collection.isEmpty()) {
          wavelets.add(collection.getWaveletName().waveletId);
        }
      }
      return wavelets;
    }
  }

  @Override
  public DeltasAccess open(WaveletName waveletName) {
    Map<WaveletId, MemoryDeltaCollection> waveData = getOrCreateWaveData(waveletName.waveId);

    MemoryDeltaCollection collection = waveData.get(waveletName.waveletId);
    if (collection == null) {
      collection = new MemoryDeltaCollection(waveletName);
      waveData.put(waveletName.waveletId, collection);
    }

    return collection;
  }
}
