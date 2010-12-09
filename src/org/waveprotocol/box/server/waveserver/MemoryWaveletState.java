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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.common.HashedVersionFactoryImpl;
import org.waveprotocol.box.server.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;

/**
 * All-in-memory implementation of wavelet state for testing.
 *
 * @author soren@google.com (Soren Lassen)
 */
class MemoryWaveletState implements WaveletState {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());

  private static final HashedVersionFactory HASH_FACTORY =
      new HashedVersionFactoryImpl(URI_CODEC);

  /**
   * @return An entry keyed by a hashed version with the given version number,
   *         if any, otherwise null.
   */
  private static <T> Map.Entry<HashedVersion, T> lookup(
      NavigableMap<HashedVersion, T> map, long version) {
    // Smallest key with version number >= version.
    HashedVersion key = HashedVersion.unsigned(version);
    Map.Entry<HashedVersion, T> entry = map.ceilingEntry(key);
    return (entry != null && entry.getKey().getVersion() == version) ? entry : null;
  }

  private final ExecutorService executor;
  private final WaveletName waveletName;
  private final HashedVersion versionZero;

  /** CopyOnWriteArraySet so we can iterate over it in another thread. */
  private final Set<PersistenceListener> persistenceListeners =
      new CopyOnWriteArraySet<PersistenceListener>();

  /** Keyed by appliedAtVersion. */
  private final NavigableMap<HashedVersion, ByteStringMessage<ProtocolAppliedWaveletDelta>>
      appliedDeltas = Maps.newTreeMap();

  /** Keyed by appliedAtVersion. */
  private final NavigableMap<HashedVersion, TransformedWaveletDelta> transformedDeltas =
      Maps.newTreeMap();

  /** Is null if the wavelet state is empty. */
  private WaveletData snapshot;

  /** Last version persisted with a call to persist(), or null if never called. */
  private HashedVersion lastPersistedVersion;

  /** Constructs an empty wavelet state with the given name. */
  public MemoryWaveletState(ExecutorService executor, WaveletName waveletName) {
    this.executor = executor;
    this.waveletName = waveletName;
    this.versionZero = HASH_FACTORY.createVersionZero(waveletName);
  }

  @Override
  public WaveletName getWaveletName() {
    return waveletName;
  }

  @Override
  public ReadableWaveletData getSnapshot() {
    return snapshot;
  }

  @Override
  public HashedVersion getCurrentVersion() {
    return (snapshot == null) ? versionZero : snapshot.getHashedVersion();
  }

  @Override
  public HashedVersion getLastPersistedVersion() {
    return (lastPersistedVersion == null) ? versionZero : lastPersistedVersion;
  }

  @Override
  public HashedVersion getHashedVersion(long version) {
    if (version == 0) {
      return versionZero;
    } else if (snapshot == null) {
      return null;
    } else if (version == snapshot.getVersion()) {
      return snapshot.getHashedVersion();
    } else {
      Map.Entry<HashedVersion, TransformedWaveletDelta> entry = lookup(transformedDeltas, version);
      return (entry == null) ? null : entry.getKey();
    }
  }

  @Override
  public TransformedWaveletDelta getTransformedDelta(HashedVersion beginVersion) {
    return transformedDeltas.get(beginVersion);
  }

  @Override
  public TransformedWaveletDelta getTransformedDeltaByEndVersion(HashedVersion endVersion) {
    Preconditions.checkArgument(endVersion.getVersion() > 0,
        "end version %s is not positive", endVersion);
    if (snapshot == null) {
      return null;
    } else if (endVersion.equals(snapshot.getHashedVersion())) {
      return transformedDeltas.lastEntry().getValue();
    } else {
      TransformedWaveletDelta delta = transformedDeltas.lowerEntry(endVersion).getValue();
      return delta.getResultingVersion().equals(endVersion) ? delta : null;
    }
  }

  @Override
  public DeltaSequence getTransformedDeltaHistory(HashedVersion startVersion,
      HashedVersion endVersion) {
    Preconditions.checkArgument(startVersion.getVersion() < endVersion.getVersion(),
        "Start version %s should be smaller than end version %s", startVersion, endVersion);
    NavigableMap<HashedVersion, TransformedWaveletDelta> deltas =
        transformedDeltas.subMap(startVersion, true, endVersion, false);
    return
        (!deltas.isEmpty() &&
         deltas.firstKey().equals(startVersion) &&
         deltas.lastEntry().getValue().getResultingVersion().equals(endVersion))
        ? DeltaSequence.of(deltas.values())
        : null;
  }

  @Override
  public ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDelta(
      HashedVersion beginVersion) {
    return appliedDeltas.get(beginVersion);
  }

  @Override
  public ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDeltaByEndVersion(
      HashedVersion endVersion) {
    Preconditions.checkArgument(endVersion.getVersion() > 0,
        "end version %s is not positive", endVersion);
    return isDeltaBoundary(endVersion) ? appliedDeltas.lowerEntry(endVersion).getValue() : null;

  }

  @Override
  public Collection<ByteStringMessage<ProtocolAppliedWaveletDelta>> getAppliedDeltaHistory(
      HashedVersion startVersion, HashedVersion endVersion) {
    Preconditions.checkArgument(startVersion.getVersion() < endVersion.getVersion());
    return (isDeltaBoundary(startVersion) && isDeltaBoundary(endVersion))
        ? appliedDeltas.subMap(startVersion, endVersion).values()
        : null;
  }

  @Override
  public void appendDelta(HashedVersion appliedAtVersion,
      TransformedWaveletDelta transformedDelta,
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta)
      throws OperationException {
    HashedVersion currentVersion = getCurrentVersion();
    Preconditions.checkArgument(currentVersion.equals(appliedAtVersion),
        "Applied version %s doesn't match current version %s", appliedAtVersion, currentVersion);

    if (appliedAtVersion.getVersion() == 0) {
      Preconditions.checkState(lastPersistedVersion == null);
      snapshot = WaveletDataUtil.buildWaveletFromFirstDelta(getWaveletName(), transformedDelta);
    } else {
      WaveletDataUtil.applyWaveletDelta(transformedDelta, snapshot);
    }

    // Now that we built the snapshot without any exceptions, we record the delta.
    transformedDeltas.put(appliedAtVersion, transformedDelta);
    appliedDeltas.put(appliedAtVersion, appliedDelta);
  }

  @Override
  public void persist(final HashedVersion version) {
    Preconditions.checkArgument(version.getVersion() > 0,
        "Cannot persist non-positive version %s", version);
    Preconditions.checkArgument(isDeltaBoundary(version),
        "Version to persist %s matches no delta", version);

    // Ignore, if this version is already persisted.
    if (lastPersistedVersion != null && lastPersistedVersion.getVersion() >= version.getVersion()) {
      return;
    }

    lastPersistedVersion = version;

    // This implementation regards things as persisted when they are in memory, so
    // we report the version as persisted as soon as we're asked to persist it.
    executor.execute(new Runnable() {
      @Override
      public void run() {
        for (PersistenceListener listener : persistenceListeners) {
          listener.persisted(waveletName, version);
        }
      }
    });
  }

  @Override
  public void close() {
    persistenceListeners.clear();
  }

  @Override
  public void addPersistenceListener(PersistenceListener listener) {
    persistenceListeners.add(listener);
  }

  @Override
  public void removePersistenceListener(PersistenceListener listener) {
    persistenceListeners.remove(listener);
  }

  private boolean isDeltaBoundary(HashedVersion version) {
    Preconditions.checkNotNull(version, "version is null");
    return version.equals(getCurrentVersion()) || transformedDeltas.containsKey(version);
  }
}
