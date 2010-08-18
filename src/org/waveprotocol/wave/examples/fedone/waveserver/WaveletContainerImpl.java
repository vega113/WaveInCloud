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
import com.google.common.collect.Sets;
import com.google.protobuf.InvalidProtocolBufferException;

import org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactory;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactoryImpl;
import org.waveprotocol.wave.examples.fedone.common.TransformedDeltaComparator;
import org.waveprotocol.wave.examples.fedone.util.Log;
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
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;
import org.waveprotocol.wave.model.wave.data.core.impl.CoreWaveletDataImpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Contains the history of a wavelet - applied and transformed deltas plus the content
 * of the wavelet.
 */
abstract class WaveletContainerImpl implements WaveletContainer {

  /**
   * A wavelet delta with a target hashed version.
   */
  protected static final class VersionedWaveletDelta {
    public final CoreWaveletDelta delta;
    public final HashedVersion version;

    public VersionedWaveletDelta(CoreWaveletDelta delta, HashedVersion version) {
      this.delta = delta;
      this.version = version;
    }
  }

  private static final Log LOG = Log.get(WaveletContainerImpl.class);

  protected static final HashedVersionFactory HASHED_HISTORY_VERSION_FACTORY =
      new HashedVersionFactoryImpl();

  protected final NavigableSet<ByteStringMessage<ProtocolAppliedWaveletDelta>> appliedDeltas;
  protected final NavigableSet<ProtocolWaveletDelta> transformedDeltas;
  protected final NavigableSet<VersionedWaveletDelta> deserializedTransformedDeltas;
  private final Lock readLock;
  private final Lock writeLock;
  protected WaveletName waveletName;
  protected CoreWaveletData waveletData;
  protected HashedVersion currentVersion;
  protected ProtocolHashedVersion lastCommittedVersion;
  protected State state;

  /** Constructor. */
  public WaveletContainerImpl(WaveletName waveletName) {
    this.waveletName = waveletName;
    waveletData = new CoreWaveletDataImpl(waveletName.waveId, waveletName.waveletId);
    currentVersion = HASHED_HISTORY_VERSION_FACTORY.createVersionZero(waveletName);
    lastCommittedVersion = null;

    appliedDeltas = Sets.newTreeSet(appliedDeltaComparator);
    transformedDeltas = Sets.newTreeSet(TransformedDeltaComparator.INSTANCE);
    deserializedTransformedDeltas = Sets.newTreeSet(deserializedDeltaComparator);

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
  protected static final Comparator<ByteStringMessage<ProtocolAppliedWaveletDelta>> appliedDeltaComparator =
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

  /**
   * Return a dummy VersionedWaveletDelta instance used as a range boundary for
   * use in searches within a NavigableSet of deltas.
   *
   * @param version the version with which to return the versioned delta
   * @return a dummy versioned delta with a null delta
   */
  private static VersionedWaveletDelta emptyDeserializedDeltaAtVersion(final long version) {
    return new VersionedWaveletDelta(null, HashedVersion.unsigned(version));
  }

  /** A comparator to be used in a TreeSet for deserialized deltas. */
  private static final Comparator<VersionedWaveletDelta> deserializedDeltaComparator =
      new Comparator<VersionedWaveletDelta>() {
        @Override
        public int compare(VersionedWaveletDelta first, VersionedWaveletDelta second) {
          if (first == null && second != null) { return -1; }
          if (first != null && second == null) { return 1; }
          if (first == null && second == null) { return 0; }
          return Long.valueOf(first.version.getVersion()).compareTo(second.version.getVersion());
        }
      };

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
  public CoreWaveletData getWaveletData() {
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
   * @param appliedVersion that the delta is applied to
   * @return the transformed delta and the version it was applied at
   *   (the version is the current version of the wavelet, unless the delta is
   *   a duplicate in which case it is the version at which it was originally
   *   applied)
   * @throws InvalidHashException if submitting against same version but different hash
   * @throws OperationException if transformation fails
   */
  protected VersionedWaveletDelta maybeTransformSubmittedDelta(CoreWaveletDelta delta,
      HashedVersion appliedVersion) throws InvalidHashException, OperationException {
    if (appliedVersion.equals(currentVersion)) {
      // Applied version is the same, we're submitting against head, don't need to do OT
      return new VersionedWaveletDelta(delta, appliedVersion);
    } else {
      // Not submitting against head, we need to do OT, but check the versions really are different
      if (appliedVersion.getVersion() == currentVersion.getVersion()) {
        LOG.warning("Same version (" + currentVersion.getVersion() + ") but different hashes (" +
            appliedVersion + "/" + currentVersion + ")");
        throw new InvalidHashException("Different hash, same version: "
            + currentVersion.getVersion());
      } else {
        return transformSubmittedDelta(delta, appliedVersion);
      }
    }
  }

  /**
   * Apply a list of operations from a single delta to the wavelet container.
   *
   * @param ops to apply
   */
  protected void applyWaveletOperations(List<CoreWaveletOperation> ops) throws OperationException,
      EmptyDeltaException {
    if (ops.isEmpty()) {
      LOG.warning("No operations to apply at version " + currentVersion);
      throw new EmptyDeltaException();
    }

    CoreWaveletOperation lastOp = null;
    int opsApplied = 0;

    try {
      for (CoreWaveletOperation op : ops) {
        lastOp = op;
        op.apply(waveletData);
        opsApplied++;
      }
    } catch (OperationException e) {
      LOG.warning("Only applied " + opsApplied + " of " + ops.size() + " operations at version "
          + currentVersion + ", rolling back, failed op was " + lastOp, e);
      // Deltas are atomic, so roll back all operations that were successful
      rollbackWaveletOperations(ops.subList(0, opsApplied));
      throw new OperationException("Failed to apply all operations, none were applied", e);
    }
  }

  /**
   * Like applyWaveletOperations, but applies the inverse of the given operations (in reverse), and
   * no operations are permitted to fail.
   *
   * @param ops to roll back
   */
  private void rollbackWaveletOperations(List<CoreWaveletOperation> ops) {
    for (int i = ops.size() - 1; i >= 0; i--) {
      try {
        ops.get(i).getInverse().apply(waveletData);
      } catch (OperationException e) {
        throw new IllegalArgumentException("Failed to roll back " + ops.get(i) + " with inverse "
            + ops.get(i).getInverse(), e);
      }
    }
  }

  /**
   * Finds range of server deltas needed to transform against, then transforms all client
   * ops against the server ops.
   */
  private VersionedWaveletDelta transformSubmittedDelta(CoreWaveletDelta submittedDelta,
      HashedVersion appliedVersion) throws OperationException, InvalidHashException {

    NavigableSet<VersionedWaveletDelta> serverDeltas = deserializedTransformedDeltas.tailSet(
        deserializedTransformedDeltas.floor(emptyDeserializedDeltaAtVersion(
            appliedVersion.getVersion())),true);

    if (serverDeltas.size() == 0) {
      LOG.warning("Got empty server set, but not sumbitting to head! " + submittedDelta);
      // Not strictly an invalid hash, but it's a related issue
      throw new InvalidHashException("Cannot submit to head");
    }

    // Confirm that the target version/hash of this delta is valid.
    if (!serverDeltas.first().version.equals(appliedVersion)) {
      LOG.warning("Mismatched hashes: expected: " + serverDeltas.first().version +
          " got: " + appliedVersion);
      // Don't leak the hash to the client in the error message.
      throw new InvalidHashException("Mismatched hashes at version " + appliedVersion.getVersion());
    }

    ParticipantId clientAuthor = submittedDelta.getAuthor();
    List<CoreWaveletOperation> clientOps = submittedDelta.getOperations();
    for (VersionedWaveletDelta d : serverDeltas) {
      // If the client delta transforms to nothing before we've traversed all the server
      // deltas, return the version at which the delta was obliterated (rather than the
      // current version) to ensure that delta submission is idempotent.
      if (clientOps.isEmpty()) {
        return new VersionedWaveletDelta(new CoreWaveletDelta(clientAuthor, clientOps), d.version);
      }
      ParticipantId serverAuthor = d.delta.getAuthor();
      List<CoreWaveletOperation> serverOps = d.delta.getOperations();
      if (clientAuthor.equals(serverAuthor) && clientOps.equals(serverOps)) {
        return d;
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
        CoreWaveletOperationSerializer.serialize(transformedDelta, currentVersion, newVersion);
    transformedDeltas.add(transformedProtocolDelta);
    deserializedTransformedDeltas.add(new VersionedWaveletDelta(transformedDelta, currentVersion));
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
  public NavigableSet<ByteStringMessage<ProtocolAppliedWaveletDelta>> requestHistory(
      ProtocolHashedVersion versionStart, ProtocolHashedVersion versionEnd)
      throws WaveletStateException {
    acquireReadLock();
    try {
      assertStateOk();
      // TODO: ### validate requested range.
      // TODO: #### make immutable.

      NavigableSet<ByteStringMessage<ProtocolAppliedWaveletDelta>> set =
          appliedDeltas.subSet(appliedDeltas.floor(
              emptyAppliedDeltaAtVersion(versionStart.getVersion())),
              true,
              emptyAppliedDeltaAtVersion(versionEnd.getVersion()),
              false);
      LOG.info("### HR " + versionStart.getVersion() + " - " + versionEnd.getVersion() + " set - " +
          set.size() + " = " + set);
      return set;
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public NavigableSet<ProtocolWaveletDelta> requestTransformedHistory(ProtocolHashedVersion versionStart,
      ProtocolHashedVersion versionEnd) throws WaveletStateException {
    acquireReadLock();
    try {
      assertStateOk();
      // TODO: ### validate requested range.
      // TODO: #### make immutable.
      return transformedDeltas.subSet(transformedDeltas.floor(
          emptyDeltaAtVersion(versionStart.getVersion())),
          true, emptyDeltaAtVersion(versionEnd.getVersion()), false);
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public List<ParticipantId> getParticipants() {
    acquireReadLock();
    try {
      return (waveletData != null ? waveletData.getParticipants() : null);
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public HashedVersion getCurrentVersion() {
    return currentVersion;
  }
}
