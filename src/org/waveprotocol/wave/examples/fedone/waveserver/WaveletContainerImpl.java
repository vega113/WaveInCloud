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

package org.waveprotocol.wave.examples.fedone.waveserver;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.InvalidProtocolBufferException;

import org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactory;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactoryImpl;
import org.waveprotocol.wave.examples.fedone.common.VersionedWaveletDelta;
import org.waveprotocol.wave.examples.fedone.util.EmptyDeltaException;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.util.WaveletDataUtil;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.core.CoreTransform;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.version.DistinctVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
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

  private static final SecureRandom RANDOM_GENERATOR = new SecureRandom();

  protected static final HashedVersionFactory HASHED_HISTORY_VERSION_FACTORY =
      new HashedVersionFactoryImpl();

  protected final NavigableSet<ByteStringMessage<ProtocolAppliedWaveletDelta>> appliedDeltas;
  private final NavigableMap<HashedVersion, ProtocolWaveletDelta> transformedDeltas =
      Maps.newTreeMap();
  private final NavigableMap<HashedVersion, CoreWaveletDelta> deserializedTransformedDeltas =
      Maps.newTreeMap();
  private final Lock readLock;
  private final Lock writeLock;
  private WaveletData waveletData;
  protected WaveletName waveletName;
  protected HashedVersion currentVersion;
  protected ProtocolHashedVersion lastCommittedVersion;
  protected State state;

  /**
   * Constructs an empty WaveletContainer for a wavelet with the given name.
   * waveletData is not set until a delta has been applied.
   *
   * @param waveletName the name of the wavelet.
   */
  public WaveletContainerImpl(WaveletName waveletName) {
    this.waveletName = waveletName;
    waveletData = null;
    currentVersion = HASHED_HISTORY_VERSION_FACTORY.createVersionZero(waveletName);
    lastCommittedVersion = null;

    appliedDeltas = Sets.newTreeSet(appliedDeltaComparator);

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

  /** A comparator to be used in a TreeSet for applied deltas. */
  protected static final Comparator<ByteStringMessage<ProtocolAppliedWaveletDelta>>
      appliedDeltaComparator =
      new Comparator<ByteStringMessage<ProtocolAppliedWaveletDelta>>() {
        @Override
        public int compare(ByteStringMessage<ProtocolAppliedWaveletDelta> first,
            ByteStringMessage<ProtocolAppliedWaveletDelta> second) {
          if (first == null && second != null) { return -1; }
          if (first != null && second == null) { return 1; }
          if (first == null && second == null) { return 0; }
          try {
            Long v1 = AppliedDeltaUtil.getHashedVersionAppliedAt(first).getVersion();
            Long v2 = AppliedDeltaUtil.getHashedVersionAppliedAt(second).getVersion();
            return v1.compareTo(v2);
          } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("Invalid applied delta added to history", e);
          }
        }
      };

  /**
   * Return a dummy ProtocolWaveletDelta instance used as a range
   * boundary for use in searches within a NavigableSet of deltas.
   *
   * @param version the version to return the delta applied at
   * @return the generated dummy delta
   */
  private static ProtocolWaveletDelta emptyDeltaAtVersion(final long version) {
    return ProtocolWaveletDelta.newBuilder()
        .setAuthor("dummy")
        .setHashedVersion(CoreWaveletOperationSerializer.serialize(HashedVersion.unsigned(version)))
        .build();
  }

  /**
   * Return a dummy ProtocolAppliedWaveleetDelta instance used as a range
   * boundary.
   */
  private static ByteStringMessage<ProtocolAppliedWaveletDelta> emptyAppliedDeltaAtVersion(
      final long version) {
    ProtocolAppliedWaveletDelta delta = ProtocolAppliedWaveletDelta.newBuilder()
        .setApplicationTimestamp(0)
        .setOperationsApplied(0)
        .setSignedOriginalDelta(ProtocolSignedDelta.newBuilder()
             .setDelta(emptyDeltaAtVersion(version).toByteString())
        ).build();
    return ByteStringMessage.fromMessage(delta);
  }

  protected void assertStateOk() throws WaveletStateException {
    if (state != State.OK) {
      throw new WaveletStateException(state, "The wavelet is not in a usable state. ");
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
      assertStateOk();
      return waveletData.getParticipants().contains(participantId);
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public ProtocolHashedVersion getLastCommittedVersion() throws WaveletStateException {
    acquireReadLock();
    try {
      assertStateOk();
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
  public <T> T getSnapshot(WaveletSnapshotBuilder<T> builder) {
    acquireWriteLock();
    try {
      return builder.build(waveletData, currentVersion, lastCommittedVersion);
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
  protected VersionedWaveletDelta maybeTransformSubmittedDelta(VersionedWaveletDelta delta)
      throws InvalidHashException, OperationException {
    HashedVersion appliedVersion = delta.version;
    if (appliedVersion.equals(currentVersion)) {
      // Applied version is the same, we're submitting against head, don't need to do OT
      return delta;
    } else {
      // Not submitting against head, we need to do OT, but check the versions really are different
      if (appliedVersion.getVersion() == currentVersion.getVersion()) {
        LOG.warning("Same version (" + currentVersion.getVersion() + ") but different hashes (" +
            appliedVersion + "/" + currentVersion + ")");
        throw new InvalidHashException("Different hash, same version: "
            + currentVersion.getVersion());
      } else {
        return transformSubmittedDelta(delta);
      }
    }
  }

  /**
   * Apply the operations from a single delta to the wavelet container.
   *
   * @param delta {@link CoreWaveletDelta} to apply.
   * @param applicationTimeStamp timestamp of the application.
   */
  protected void applyWaveletOperations(CoreWaveletDelta delta, long applicationTimeStamp)
      throws OperationException, EmptyDeltaException {
    if (delta.getOperations().isEmpty()) {
      throw new EmptyDeltaException(
          "No operations to apply at version " + currentVersion.getVersion());
    }

    if (waveletData == null) {
      Preconditions.checkState(currentVersion.getVersion() == 0L, "CurrentVersion must be 0");
      waveletData =
          WaveletDataUtil.createEmptyWavelet(waveletName, delta.getAuthor(), applicationTimeStamp);
    }

    DistinctVersion endVersion = DistinctVersion.of(
        waveletData.getVersion() + delta.getOperations().size(), RANDOM_GENERATOR.nextInt());
    WaveletDataUtil.applyWaveletDelta(delta, waveletData, endVersion, applicationTimeStamp);
  }

  /**
   * Finds range of server deltas needed to transform against, then transforms all client
   * ops against the server ops.
   */
  private VersionedWaveletDelta transformSubmittedDelta(
      VersionedWaveletDelta versionedSubmittedDelta)
      throws OperationException, InvalidHashException {
    CoreWaveletDelta submittedDelta = versionedSubmittedDelta.delta;
    HashedVersion appliedVersion = versionedSubmittedDelta.version;
    NavigableMap<HashedVersion, CoreWaveletDelta> serverDeltas =
        deserializedTransformedDeltas.tailMap(
            deserializedTransformedDeltas.floorKey(appliedVersion),
            true);

    if (serverDeltas.isEmpty()) {
      LOG.warning("Got empty server set, but not sumbitting to head! " + submittedDelta);
      // Not strictly an invalid hash, but it's a related issue
      throw new InvalidHashException("Cannot submit to head");
    }

    // Confirm that the target version/hash of this delta is valid.
    if (!serverDeltas.firstEntry().getKey().equals(appliedVersion)) {
      LOG.warning("Mismatched hashes: expected: " + serverDeltas.firstEntry().getKey() +
          " got: " + appliedVersion);
      // Don't leak the hash to the client in the error message.
      throw new InvalidHashException("Mismatched hashes at version " + appliedVersion.getVersion());
    }

    ParticipantId clientAuthor = submittedDelta.getAuthor();
    List<CoreWaveletOperation> clientOps = submittedDelta.getOperations();
    for (Map.Entry<HashedVersion, CoreWaveletDelta> d : serverDeltas.entrySet()) {
      // If the client delta transforms to nothing before we've traversed all the server
      // deltas, return the version at which the delta was obliterated (rather than the
      // current version) to ensure that delta submission is idempotent.
      if (clientOps.isEmpty()) {
        return new VersionedWaveletDelta(new CoreWaveletDelta(clientAuthor, clientOps), d.getKey());
      }
      CoreWaveletDelta coreDelta = d.getValue();
      ParticipantId serverAuthor = coreDelta.getAuthor();
      List<CoreWaveletOperation> serverOps = coreDelta.getOperations();
      if (clientAuthor.equals(serverAuthor) && clientOps.equals(serverOps)) {
        return new VersionedWaveletDelta(coreDelta, d.getKey());
      }
      clientOps = transformOps(clientOps, clientAuthor, serverOps, serverAuthor);
    }
    return new VersionedWaveletDelta(new CoreWaveletDelta(clientAuthor, clientOps), currentVersion);
  }

  /**
   * Transforms the specified client operations against the specified server operations,
   * returning the transformed client operations in a new list.
   *
   * @param clientOps may be unmodifiable
   * @param clientAuthor
   * @param serverOps may be unmodifiable
   * @param serverAuthor
   * @return The transformed client ops
   */
  private List<CoreWaveletOperation> transformOps(
      List<CoreWaveletOperation> clientOps, ParticipantId clientAuthor,
      List<CoreWaveletOperation> serverOps, ParticipantId serverAuthor) throws OperationException {
    List<CoreWaveletOperation> transformedClientOps = new ArrayList<CoreWaveletOperation>();

    for (CoreWaveletOperation c : clientOps) {
      for (CoreWaveletOperation s : serverOps) {
        OperationPair<CoreWaveletOperation> pair;
        try {

          pair = CoreTransform.transform(c, clientAuthor, s, serverAuthor);
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
   * Commit an applied delta to this wavelet container.
   *
   * @param appliedDelta to commit
   * @param transformedDelta of the applied delta
   * @return result of the application
   */
  protected DeltaApplicationResult commitAppliedDelta(
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta,
      CoreWaveletDelta transformedDelta) {
    int operationsApplied = appliedDelta.getMessage().getOperationsApplied();
    // Sanity check.
    Preconditions.checkArgument(operationsApplied == transformedDelta.getOperations().size());

    HashedVersion newVersion = HASHED_HISTORY_VERSION_FACTORY.create(
        appliedDelta.getByteArray(), currentVersion, operationsApplied);

    ProtocolWaveletDelta transformedProtocolDelta =
        CoreWaveletOperationSerializer.serialize(transformedDelta, currentVersion);
    transformedDeltas.put(currentVersion, transformedProtocolDelta);
    deserializedTransformedDeltas.put(currentVersion, transformedDelta);
    appliedDeltas.add(appliedDelta);

    currentVersion = newVersion;

    return new DeltaApplicationResult(appliedDelta, transformedProtocolDelta,
        CoreWaveletOperationSerializer.serialize(newVersion));
  }

  /**
   * Returns the applied delta that was applied at a given hashed version.
   *
   * @param version the version to look up
   * @return the applied delta applied at the specified hashed version
   */
  protected ByteStringMessage<ProtocolAppliedWaveletDelta> lookupAppliedDelta(
      HashedVersion version) {
    return appliedDeltas.floor(emptyAppliedDeltaAtVersion(version.getVersion()));
  }

  @Override
  public Collection<ByteStringMessage<ProtocolAppliedWaveletDelta>> requestHistory(
      ProtocolHashedVersion versionStart, ProtocolHashedVersion versionEnd)
      throws WaveletStateException {
    acquireReadLock();
    try {
      assertStateOk();
      // TODO: ### validate requested range.
      // TODO: #### make immutable.

      Set<ByteStringMessage<ProtocolAppliedWaveletDelta>> set =
          appliedDeltas.subSet(
              appliedDeltas.floor(
                  emptyAppliedDeltaAtVersion(versionStart.getVersion())),
              emptyAppliedDeltaAtVersion(versionEnd.getVersion()));
      LOG.info("### HR " + versionStart.getVersion() + " - " + versionEnd.getVersion() + " set - " +
          set.size() + " = " + set);
      return set;
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public Collection<ProtocolWaveletDelta> requestTransformedHistory(
      ProtocolHashedVersion versionStart, ProtocolHashedVersion versionEnd)
      throws WaveletStateException {
    HashedVersion start = CoreWaveletOperationSerializer.deserialize(versionStart);
    HashedVersion end = CoreWaveletOperationSerializer.deserialize(versionEnd);
    acquireReadLock();
    try {
      assertStateOk();
      // TODO: ### validate requested range.
      // TODO: #### make immutable.
      return transformedDeltas.subMap(transformedDeltas.floorKey(start), end).values();
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

  @Override
  public HashedVersion getCurrentVersion() {
    return currentVersion;
  }
}
