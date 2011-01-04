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

package org.waveprotocol.box.server.waveserver;

import com.google.common.collect.ImmutableSet;

import org.waveprotocol.box.server.persistence.FileNotFoundPersistenceException;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.io.Closeable;
import java.util.Collection;

/**
 * Stores wavelets.
 *
 * <p>
 * Callers must serialize all calls to {@link #open(WaveletName)} and
 * {@link #delete(WaveletName)} on the same wavelet.
 *
 * @author soren@google.com (Soren Lassen)
 */
public interface WaveletStore {

  /**
   * Accesses the state for a wavelet.
   * Permits reading a snapshot of the wavelet state and historical deltas,
   * and appending deltas to the history.
   *
   * Callers must serialize all calls to
   * {@link #appendDeltas(Collection,ReadableWaveletData)}.
   */
  interface WaveletAccess extends WaveletDeltaRecordReader, Closeable {

    /**
     * @return Immutable copy of the last known committed state of the wavelet.
     */
    ReadableWaveletData getSnapshot();

    /**
     * Blocking call to append deltas to the end of the delta history.
     * If the call returns normally (doesn't throw an exception), then
     * the deltas have been successfully and "durably" stored, that is,
     * the method forces the data to disk.
     *
     * @param deltas Contiguous deltas, beginning from the DeltaAccess object's
     *        end version. It is the caller's responsibility to ensure that
     *        everything matches up (applied and transformed deltas in the
     *        records match, that the hashes are correctly computed, etc).
     * @param resultingSnapshot A snapshot of the state of the wavelet after
     *        {@code deltas} are applied.
     * @throws PersistenceException if anything goes wrong with the underlying
     *         storage.
     */
    void appendDeltas(Collection<WaveletDeltaRecord> deltas,
        ReadableWaveletData resultingSnapshot) throws PersistenceException;
  }

  /**
   * Opens a wavelet, which can be used to store deltas. If the wavelet doesn't
   * exist, it is implicitly created when the first op is appended to it.
   *
   * @throws PersistenceException if anything goes wrong with the underlying storage.
   */
  WaveletAccess open(WaveletName waveletName) throws PersistenceException;

  /**
   * Deletes a non-empty wavelet.
   *
   * @throws PersistenceException if anything goes wrong with the underlying storage.
   * @throws FileNotFoundPersistenceException if this wavelet doesn't exist in
   *         the wavelet store.
   */
  void delete(WaveletName waveletName) throws PersistenceException,
      FileNotFoundPersistenceException;

  /**
   * Looks up all wavelets with deltas in the wavelet store.
   *
   * @throws PersistenceException if anything goes wrong with the underlying storage.
   */
  ImmutableSet<WaveletId> lookup(WaveId waveId) throws PersistenceException;
}
