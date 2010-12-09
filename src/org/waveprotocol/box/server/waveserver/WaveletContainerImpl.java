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

package org.waveprotocol.box.server.waveserver;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.common.HashedVersionFactoryImpl;
import org.waveprotocol.box.server.common.SnapshotSerializer;
import org.waveprotocol.box.server.frontend.WaveletSnapshotAndVersion;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.box.server.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.Transform;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Contains the history of a wavelet - applied and transformed deltas plus the content
 * of the wavelet.
 */
abstract class WaveletContainerImpl implements WaveletContainer {

  private static final Log LOG = Log.get(WaveletContainerImpl.class);

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());

  private static final HashedVersionFactory HASH_FACTORY =
      new HashedVersionFactoryImpl(URI_CODEC);

  /** Keyed by appliedAtVersion */
  private final NavigableMap<HashedVersion, ByteStringMessage<ProtocolAppliedWaveletDelta>>
      appliedDeltas = Maps.newTreeMap();
  /** Keyed by appliedAtVersion */
  private final NavigableMap<HashedVersion, TransformedWaveletDelta> transformedDeltas =
      Maps.newTreeMap();
  private final Lock readLock;
  private final Lock writeLock;
  private WaveletData waveletData;
  private WaveletName waveletName;
  private HashedVersion currentVersion;
  protected HashedVersion lastCommittedVersion;
  protected State state;

  /**
   * Constructs an empty WaveletContainer for a wavelet.
   * waveletData is not set until a delta has been applied.
   *
   * @param waveletAccess access to the wavelet in the wavelet store
   */
  public WaveletContainerImpl(WaveletStore.WaveletAccess waveletAccess) {
    // TODO(soren): make proper use of waveletAccess
    this.waveletName = waveletAccess.getWaveletName();
    waveletData = null;
    currentVersion = HASH_FACTORY.createVersionZero(waveletName);
    lastCommittedVersion = null;

    // Configure the locks used by this Wavelet.
    final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    readLock = readWriteLock.readLock();
    writeLock = readWriteLock.writeLock();
    state = State.OK;
  }

  protected void acquireReadLock() {
    readLock.lock();
  }

  protected void releaseReadLock() {
    readLock.unlock();
  }

  protected void acquireWriteLock() {
    writeLock.lock();
  }

  protected void releaseWriteLock() {
    writeLock.unlock();
  }

  protected WaveletName getWaveletName() {
    return waveletName;
  }

  protected void checkStateOk() throws WaveletStateException {
    if (state != State.OK) {
      throw new WaveletStateException(state, "The wavelet is in an unusable state: " + state);
    }
  }

  @Override
  public State getState() {
    acquireReadLock();
    try {
      return state;
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public void setState(State state) {
    acquireWriteLock();
    try {
      this.state = state;
    } finally {
      releaseWriteLock();
    }
  }

  @Override
  public boolean checkAccessPermission(ParticipantId participantId) throws WaveletStateException {
    acquireReadLock();
    try {
      checkStateOk();
      // ParticipantId will be null if the user isn't logged in. A user who isn't logged in should
      // have access to public waves once they've been implemented.
      return participantId != null && waveletData.getParticipants().contains(participantId);
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public HashedVersion getLastCommittedVersion() throws WaveletStateException {
    acquireReadLock();
    try {
      checkStateOk();
      return lastCommittedVersion;
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public WaveletData getWaveletData() {
    return waveletData;
  }

  @Override
  public WaveletSnapshotAndVersion getSnapshot() {
    acquireWriteLock();
    try {
      HashedVersion committedVersion = currentVersion;
      return new WaveletSnapshotAndVersion(
          SnapshotSerializer.serializeWavelet(waveletData, currentVersion),
          CoreWaveletOperationSerializer.serialize(committedVersion));
    } finally {
      releaseWriteLock();
    }
  }

  /**
   * Transform a wavelet delta if it has been submitted against a different head (currentVersion).
   * Must be called with write lock held.
   *
   * @param delta to possibly transform
   * @return the transformed delta and the version it was applied at
   *   (the version is the current version of the wavelet, unless the delta is
   *   a duplicate in which case it is the version at which it was originally
   *   applied)
   * @throws InvalidHashException if submitting against same version but different hash
   * @throws OperationException if transformation fails
   */
  protected WaveletDelta maybeTransformSubmittedDelta(WaveletDelta delta)
      throws InvalidHashException, OperationException {
    HashedVersion targetVersion = delta.getTargetVersion();
    if (targetVersion.equals(currentVersion)) {
      // Applied version is the same, we're submitting against head, don't need to do OT
      return delta;
    } else {
      // Not submitting against head, we need to do OT, but check the versions really are different
      if (targetVersion.getVersion() == currentVersion.getVersion()) {
        LOG.warning("Mismatched hash, expected " + currentVersion + ") but delta targets (" +
            targetVersion + ")");
        throw new InvalidHashException(currentVersion, targetVersion);
      } else {
        return transformSubmittedDelta(delta);
      }
    }
  }

  /**
   * Finds range of server deltas needed to transform against, then transforms all client
   * ops against the server ops.
   */
  private WaveletDelta transformSubmittedDelta(WaveletDelta submittedDelta)
      throws OperationException, InvalidHashException {
    HashedVersion targetVersion = submittedDelta.getTargetVersion();
    NavigableMap<HashedVersion, TransformedWaveletDelta> serverDeltas =
        transformedDeltas.tailMap(transformedDeltas.floorKey(targetVersion), true);

    if (serverDeltas.isEmpty()) {
      LOG.warning("Got empty server set, but not sumbitting to head! " + submittedDelta);
      // Not strictly an invalid hash, but it's a related issue
      throw new InvalidHashException(HashedVersion.unsigned(0), targetVersion);
    }

    // Confirm that the target version/hash of this delta is valid.
    if (!serverDeltas.firstEntry().getKey().equals(targetVersion)) {
      LOG.warning("Mismatched hashes: expected: " + serverDeltas.firstEntry().getKey() +
          " got: " + targetVersion);
      throw new InvalidHashException(serverDeltas.firstEntry().getKey(), targetVersion);
    }

    ParticipantId clientAuthor = submittedDelta.getAuthor();
    // TODO(anorth): remove this copy somehow; currently, it's necessary to
    // ensure that clientOps.equals() works correctly below (because
    // WaveletDelta breaks the List.equals() contract)
    List<WaveletOperation> clientOps = Lists.newArrayList(submittedDelta);
    for (Map.Entry<HashedVersion, TransformedWaveletDelta> d : serverDeltas.entrySet()) {
      // If the client delta transforms to nothing before we've traversed all
      // the server deltas, return the version at which the delta was
      // obliterated (rather than the current version) to ensure that delta
      // submission is idempotent.
      if (clientOps.isEmpty()) {
        return new WaveletDelta(clientAuthor, d.getKey(), clientOps);
      }
      TransformedWaveletDelta serverDelta = d.getValue();
      ParticipantId serverAuthor = serverDelta.getAuthor();
      if (clientAuthor.equals(serverAuthor) && clientOps.equals(serverDelta)) {
        // This is a duplicate, return the server delta.
        return new WaveletDelta(serverAuthor, d.getKey(), serverDelta);
      }
      clientOps = transformOps(clientOps, serverDelta);
    }
    return new WaveletDelta(clientAuthor, currentVersion, clientOps);
  }

  /**
   * Transforms the specified client operations against the specified server operations,
   * returning the transformed client operations in a new list.
   *
   * @param clientOps may be unmodifiable
   * @param serverOps may be unmodifiable
   * @return transformed client ops
   */
  private List<WaveletOperation> transformOps(List<WaveletOperation> clientOps,
      List<WaveletOperation> serverOps) throws OperationException {
    List<WaveletOperation> transformedClientOps = Lists.newArrayList();

    for (WaveletOperation c : clientOps) {
      for (WaveletOperation s : serverOps) {
        OperationPair<WaveletOperation> pair;
        try {
          pair = Transform.transform(c, s);
        } catch (TransformException e) {
          throw new OperationException(e);
        }
        c = pair.clientOp();
      }
      transformedClientOps.add(c);
    }
    return transformedClientOps;
  }

  /**
   * Builds a {@link WaveletDeltaRecord} and applies it to the wavelet container.
   * The delta must be non-empty.
   */
  protected WaveletDeltaRecord applyDelta(
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta, WaveletDelta transformed)
      throws InvalidProtocolBufferException, OperationException {
    WaveletDeltaRecord applicationResult =
        new WaveletDeltaRecord(transformed.getTargetVersion(), appliedDelta,
            AppliedDeltaUtil.buildTransformedDelta(appliedDelta, transformed));

    // Apply the delta to the local wavelet state.
    applyDelta(applicationResult);

    return applicationResult;
  }

  /**
   * Applies the operations from a single delta to the wavelet container.
   *
   * @param delta to apply, must be non-empty.
   */
  private void applyDelta(WaveletDeltaRecord delta)
      throws InvalidProtocolBufferException, OperationException {
    ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDeltaBytes = delta.getAppliedDelta();
    ProtocolAppliedWaveletDelta appliedDelta = appliedDeltaBytes.getMessage();
    TransformedWaveletDelta transformedDelta = delta.getTransformedDelta();

    // Sanity checks.
    HashedVersion appliedAtVersion = delta.getAppliedAtVersion();
    Preconditions.checkState(currentVersion.equals(appliedAtVersion),
        "current version %s != applied at version %s",
        // we omit the hashes to avoid leaking them
        currentVersion.getVersion(), appliedAtVersion.getVersion());
    Preconditions.checkArgument(
        appliedDelta.getOperationsApplied() == transformedDelta.size());
    Preconditions.checkArgument(
        transformedDelta.size() != 0, "empty delta");

    if (waveletData == null) {
      Preconditions.checkState(currentVersion.getVersion() == 0L, "CurrentVersion must be 0");
      waveletData = WaveletDataUtil.buildWaveletFromFirstDelta(waveletName, transformedDelta);
    } else {
      // TODO(soren): fix tests and then strengthen this test:
      // Preconditions.checkState(waveletData.getHashedVersion().equals(currentVersion));
      Preconditions.checkState(waveletData.getVersion() == currentVersion.getVersion());
      WaveletDataUtil.applyWaveletDelta(transformedDelta, waveletData);
    }

    transformedDeltas.put(currentVersion, transformedDelta);
    appliedDeltas.put(currentVersion, appliedDeltaBytes);

    currentVersion = transformedDelta.getResultingVersion();
  }

  /**
   * @param versionActuallyAppliedAt the version to look up
   * @return the applied delta applied at the specified hashed version
   */
  protected ByteStringMessage<ProtocolAppliedWaveletDelta> lookupAppliedDelta(
      HashedVersion versionActuallyAppliedAt) {
    return appliedDeltas.get(versionActuallyAppliedAt);
  }

  /**
   * @param endVersion the version to look up
   * @return the applied delta with the given resulting version
   */
  protected ByteStringMessage<ProtocolAppliedWaveletDelta> lookupAppliedDeltaByEndVersion(
      HashedVersion endVersion) {
    // It's cheaper to find the resulting version of a TransformedWaveletDelta
    // than a ProtocolAppliedWaveletDelta, therefore we search for the
    // begin version in the transformedDeltas map.
    Map.Entry<HashedVersion, TransformedWaveletDelta> lower =
        transformedDeltas.lowerEntry(endVersion);
    if (lower == null || !lower.getValue().getResultingVersion().equals(endVersion)) {
      return null;
    }
    // We found the begin version so now we can find the applied delta in the appliedDeltas map.
    return appliedDeltas.get(lower.getKey());
  }

  protected TransformedWaveletDelta lookupTransformedDelta(HashedVersion appliedAtVersion) {
    return transformedDeltas.get(appliedAtVersion);
  }

  /**
   * @throws AccessControlException with the given message if version does not
   *         match a delta boundary in the wavelet history.
   */
  private void checkVersionIsDeltaBoundary(HashedVersion version, String message)
      throws AccessControlException {
    if (!transformedDeltas.containsKey(version) && !version.equals(currentVersion)) {
      // We omit the hash from the message to avoid leaking it.
      throw new AccessControlException(
          "Unrecognized " + message + " at version " + version.getVersion());
    }
  }

  @Override
  public Collection<ByteStringMessage<ProtocolAppliedWaveletDelta>> requestHistory(
      HashedVersion versionStart, HashedVersion versionEnd)
      throws AccessControlException, WaveletStateException {
    acquireReadLock();
    try {
      checkStateOk();
      checkVersionIsDeltaBoundary(versionStart, "start version");
      checkVersionIsDeltaBoundary(versionEnd, "end version");
      Collection<ByteStringMessage<ProtocolAppliedWaveletDelta>> result =
          ImmutableList.copyOf(appliedDeltas.subMap(versionStart, versionEnd).values());
      LOG.info("### HR " + versionStart.getVersion() + " - " + versionEnd.getVersion() + ", " +
          result.size() + " deltas");
      return result;
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public Collection<TransformedWaveletDelta> requestTransformedHistory(HashedVersion versionStart,
      HashedVersion versionEnd) throws AccessControlException, WaveletStateException {
    acquireReadLock();
    try {
      checkStateOk();
      checkVersionIsDeltaBoundary(versionStart, "start version");
      checkVersionIsDeltaBoundary(versionEnd, "end version");
      return ImmutableList.copyOf(transformedDeltas.subMap(versionStart, versionEnd).values());
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public Set<ParticipantId> getParticipants() {
    acquireReadLock();
    try {
      return (waveletData != null ? waveletData.getParticipants() : ImmutableSet.<
          ParticipantId>of());
    } finally {
      releaseReadLock();
    }
  }

  protected HashedVersion getCurrentVersion() {
    return currentVersion;
  }
}
