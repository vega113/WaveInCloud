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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import org.waveprotocol.box.server.persistence.FileNotFoundPersistenceException;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.waveserver.DeltaStore;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Map;
import java.util.Set;

/**
 * A simple in-memory implementation of DeltaStore.
 *
 * @author josephg@google.com (Joseph Gentle)
 */
public class MemoryDeltaStore implements DeltaStore {
  /**
   * The actual data.
   * Note: We don't remove map entries in the top-level map when all wavelets in a wave are deleted.
   * This is a very small memory leak that won't cause problems in practice.
   */
  private final Map<WaveId, Map<WaveletId, MemoryDeltaCollection>> data =
      CollectionUtils.newHashMap();

  private Map<WaveletId, MemoryDeltaCollection> getOrCreateWaveData(WaveId waveId) {
    Map<WaveletId, MemoryDeltaCollection> waveData = data.get(waveId);
    if (waveData == null) {
      waveData = CollectionUtils.newHashMap();
      data.put(waveId, waveData);
    }

    return waveData;
  }

  @Override
  public void delete(WaveletName waveletName) throws PersistenceException {
    Map<WaveletId, MemoryDeltaCollection> waveData = data.get(waveletName.waveId);
    if (waveData == null) {
      throw new FileNotFoundPersistenceException("WaveletData unknown");
    } else {
      if (waveData.remove(waveletName.waveletId) == null) {
        // Nothing was removed.
        throw new FileNotFoundPersistenceException("Nothing was removed");
      }
    }
  }

  @Override
  public Set<WaveletId> lookup(WaveId waveId) {
    Map<WaveletId, MemoryDeltaCollection> waveData = data.get(waveId);
    if (waveData == null) {
      return ImmutableSet.of();
    } else {
      Builder<WaveletId> builder = ImmutableSet.builder();
      for (MemoryDeltaCollection collection : waveData.values()) {
        if (!collection.isEmpty()) {
          builder.add(collection.getWaveletName().waveletId);
        }
      }
      return builder.build();
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
