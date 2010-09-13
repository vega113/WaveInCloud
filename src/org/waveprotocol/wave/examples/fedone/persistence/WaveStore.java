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

package org.waveprotocol.wave.examples.fedone.persistence;

import com.google.common.util.concurrent.ListenableFuture;

import org.waveprotocol.wave.examples.fedone.common.WaveletAndVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Interface for the storage and retrieval of Wavelets and Deltas.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public interface WaveStore {

  /**
   * Append a list of deltas to the wavelet in storage with the given name and
   * store a new snapshot.
   *
   *<p>
   * This operation must be implemented atomically with respect to calls to
   * getSnapshot() and getDelas() for the same wavelet.
   *
   *<p>
   * The first delta's target history hash MUST match the end hash of the last
   * delta already stored for this wavelet.
   *
   *<p>
   * Data has been persisted when the returned future completes with success.
   *
   * @param snapshot up-to-date snapshot of the wavelet to update with the given
   * @param deltas list of deltas to append.
   * @return {@link ListenableFuture} which is done when the write succeeds or fails. A
   *         failure is reported as an {@link ExecutionException} wrapping a
   *         {@link WaveStoreException}.
   */
  public ListenableFuture<?> appendWaveletDeltas(WaveletAndVersion snapshot,
      List<ProtocolWaveletDelta> deltas);

  /**
   * Retrieve a list of deltas that have been applied to the wavelet with the
   * given name.
   *
   *<p>
   * If the result exceeds the limit on the total size of deltas to return then
   * the implementor must return at most all deltas from versionStart up to but
   * not including the delta that causes the limit to be exceeded.
   *
   * @param waveletName name of wavelet.
   * @param versionStart start version (inclusive), on delta boundary, minimum
   *        0.
   * @param versionEnd end version (exclusive), on delta boundary.
   * @param maxBytes the maximum amount of bytes to be returned. Note that the
   *        implementor may set an internal maximum to return that is lower then
   *        the given maximum.
   * @return deltas in the range as requested, ordered by applied version. If an
   *         error has occurred the {@link ListenableFuture} will report this as an
   *         {@link ExecutionException}.
   *
   */
  public ListenableFuture<List<ProtocolWaveletDelta>> getDeltas(WaveletName waveletName,
      ProtocolHashedVersion versionStart, ProtocolHashedVersion versionEnd, long maxBytes);

  /**
   * Get the current state of a wavelet.
   *
   * @param waveletName the name of the wavelet.
   * @return the wavelet as {@link CoreWaveletData} or null if the wavelet
   *         doesn't exist.
   */
  public ListenableFuture<WaveletAndVersion> getSnapshot(WaveletName waveletName);

  /**
   * Returns all wavelets that the user is a participant on.
   *
   * @param participant the participant to get the inbox for.
   */
  public ListenableFuture<Iterator<WaveletAndVersion>> findWaveletsWithParticipant(
      ParticipantId participant);

  // TODO(ljvderijk): Define search
  // public ListenableFuture<List<WaveletSnapshot>> search();
}
