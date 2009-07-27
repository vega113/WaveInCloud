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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactory;
import org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.model.util.HashedVersionFactoryImpl;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.Transform;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.protocol.common.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.protocol.common.ProtocolHashedVersion;
import org.waveprotocol.wave.protocol.common.ProtocolSignedDelta;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

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
 *
 *
 *
 */
abstract class WaveletContainerImpl implements WaveletContainer {

  /**
   * A wavelet delta with a target hashed version.
   */
  private static final class VersionedWaveletDelta {
    public final WaveletDelta delta;
    public final HashedVersion version;

    public VersionedWaveletDelta(WaveletDelta delta, HashedVersion version) {
      this.delta = delta;
      this.version = version;
    }
  }

  private static final Log LOG = Log.get(WaveletContainerImpl.class);

  private static final HashedVersionFactory HASHED_HISTORY_VERSION_FACTORY =
      new HashedVersionFactoryImpl();

  // TODDO(jochen,soren): replace the appliedDeltas NavigableSet with byte[] list.
  protected final NavigableSet<ProtocolAppliedWaveletDelta> appliedDeltas;
  protected final NavigableSet<ProtocolWaveletDelta> transformedDeltas;
  protected final NavigableSet<VersionedWaveletDelta> deserializedTransformedDeltas;
  private final Lock readLock;
  private final Lock writeLock;
  protected WaveletName waveletName;
  protected WaveletData waveletData;
  protected HashedVersion currentVersion;
  protected ProtocolHashedVersion lastCommittedVersion;
  protected State state;


  /** Constructor. */
  public WaveletContainerImpl(WaveletName waveletName) {
    this.waveletName = waveletName;
    waveletData = new WaveletDataImpl(waveletName.waveId, waveletName.waveletId);
    currentVersion = HASHED_HISTORY_VERSION_FACTORY.createVersionZero(waveletName);
    lastCommittedVersion = null;

    appliedDeltas = Sets.newTreeSet(appliedDeltaComparator);
    transformedDeltas = Sets.newTreeSet(transformedDeltaComparator);
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
  protected static final Comparator<ProtocolAppliedWaveletDelta> appliedDeltaComparator =
      new Comparator<ProtocolAppliedWaveletDelta>() {
        @Override
        public int compare(ProtocolAppliedWaveletDelta first, ProtocolAppliedWaveletDelta second) {
          if (first == null && second != null) { return -1; }
          if (first != null && second == null) { return 1; }
          if (first == null && second == null) { return 0; }
          return Long.valueOf(getVersionAppliedAt(first).getVersion()).compareTo(
              getVersionAppliedAt(second).getVersion());
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
        .setHashedVersion(WaveletOperationSerializer.serialize(HashedVersion.unsigned(version)))
        .build();
  }

  /**
   * Return a dummy ProtocolAppliedWaveleetDelta instance used as a range
   * boundary.
   */
  private static ProtocolAppliedWaveletDelta emptyAppliedDeltaAtVersion(final long version) {
    return ProtocolAppliedWaveletDelta.newBuilder()
        .setApplicationTimestamp(0)
        .setOperationsApplied(0)
        .setSignedOriginalDelta(ProtocolSignedDelta.newBuilder()
             .setDelta(emptyDeltaAtVersion(version))
        ).build();
  }

  /**
   * Returns the version that the passed delta was actually applied at. Simply
   * resolves to the hashed version stored on the applied delta if it exists, or
   * defaults to the hashed version stored on the original immutable delta.
   *
   * @param appliedDelta the delta to examine
   * @return the hashed version applied at
   */
  private static ProtocolHashedVersion getVersionAppliedAt(
      final ProtocolAppliedWaveletDelta appliedDelta) {
    return appliedDelta.hasHashedVersionAppliedAt() ? appliedDelta.getHashedVersionAppliedAt()
        : appliedDelta.getSignedOriginalDelta().getDelta().getHashedVersion();
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

  /** A comparator to be used in a TreeSet for transformed deltas. */
  @VisibleForTesting
  static final Comparator<ProtocolWaveletDelta> transformedDeltaComparator =
      new Comparator<ProtocolWaveletDelta>() {
        @Override
        public int compare(ProtocolWaveletDelta first, ProtocolWaveletDelta second) {
          if (first == null && second != null) { return -1; }
          if (first != null && second == null) { return 1; }
          if (first == null && second == null) { return 0; }
          return Long.valueOf(first.getHashedVersion().getVersion()).compareTo(
              second.getHashedVersion().getVersion());
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
  public WaveletData getWaveletData() {
    return waveletData;
  }

  /**
   * This assumes the caller has validated that the delta is at the correct
   * version and can be applied to the wavelet. Must be called with writelock
   * held.
   *
   * @param signedDelta the delta that is to be applied to wavelet.
   * @return transformed operations are applied to this delta.
   * @throws OperationException if an error occurs during transformation or application
   * @throws AccessControlException if the supplied Delta's historyHash does not match the canonical
   *                                history.
   *
   * @throws OperationException
   */
  protected DeltaApplicationResult transformAndApplyDelta(ProtocolSignedDelta signedDelta)
    throws OperationException, AccessControlException {

    ProtocolWaveletDelta submittedDelta = signedDelta.getDelta();
    List<WaveletOperation> transformedClientOps;

    if (submittedDelta.getHashedVersion().getVersion() == currentVersion.getVersion()) {

      Pair<WaveletDelta, HashedVersion> clientDeltaAndVersion =
        WaveletOperationSerializer.deserialize(submittedDelta);

// TODO(jochen): fix this with byte array.
//      if (!currentVersion.equals(clientDeltaAndVersion.second)) {
//        LOG.warning("Mismatched hashes: expected " + currentVersion +
//            " got: " + clientDeltaAndVersion.second);
//        throw new AccessControlException("Mismatched hashes at version number: " +
//            clientDeltaAndVersion.second.getVersion());
//      }

      // No need to transform if submitting against head.
      transformedClientOps = clientDeltaAndVersion.first.getOperations();
    } else {
      transformedClientOps = transformSubmittedDelta(signedDelta);
    }

    // If we get here with no exceptions, the entire delta was transformed successfully.
    int applied = 0;
    String error = null;
    try {
      for (WaveletOperation op : transformedClientOps) {
          op.apply(waveletData);
          applied += 1; // Single ops are only of length one.
      }
    } catch (OperationException e) {
      error = e.getMessage();
    }

    WaveletDelta transformedDelta = new WaveletDelta(new ParticipantId(submittedDelta.getAuthor()),
        transformedClientOps);

    // Serialize applied delta with the old "current" version.
    ProtocolWaveletDelta protocolDelta = WaveletOperationSerializer.serialize(transformedDelta,
        currentVersion);
    ProtocolAppliedWaveletDelta appliedDelta = ProtocolAppliedWaveletDelta.newBuilder()
        .setSignedOriginalDelta(signedDelta)
        .setHashedVersionAppliedAt(protocolDelta.getHashedVersion())
        .setOperationsApplied(applied)
        .setApplicationTimestamp(System.currentTimeMillis()).build();

    byte[] binaryDelta = getCanonicalEncoding(appliedDelta);

    // Update current version.
    HashedVersion oldVersion = currentVersion;
    currentVersion = HASHED_HISTORY_VERSION_FACTORY.create(binaryDelta, currentVersion,
        appliedDelta.getOperationsApplied());

    appliedDeltas.add(appliedDelta);
    transformedDeltas.add(protocolDelta);
    deserializedTransformedDeltas.add(new VersionedWaveletDelta(transformedDelta, oldVersion));

    return new DeltaApplicationResult(appliedDelta,
                                      ByteString.copyFrom(binaryDelta),
                                      protocolDelta,
                                      WaveletOperationSerializer.serialize(currentVersion),
                                      error);
  }

  /**
   * Finds range of server deltas needed to transform against, then transforms all client
   * ops against the server ops.
   */
  private List<WaveletOperation> transformSubmittedDelta(ProtocolSignedDelta signedDelta)
      throws AccessControlException, OperationException {
    ProtocolWaveletDelta submittedDelta = signedDelta.getDelta();
    Pair<WaveletDelta, HashedVersion> clientDeltaAndVersion =
        WaveletOperationSerializer.deserialize(submittedDelta);
    HashedVersion clientVersion = clientDeltaAndVersion.second;

    List<WaveletOperation> clientOps = clientDeltaAndVersion.first.getOperations();

    NavigableSet<VersionedWaveletDelta> serverDeltas = deserializedTransformedDeltas.tailSet(
        deserializedTransformedDeltas.floor(emptyDeserializedDeltaAtVersion(
            clientVersion.getVersion())),true);

    if (serverDeltas.size() == 0) {
      LOG.warning("Got empty server set, but not sumbitting to head! " + signedDelta);
      throw new AccessControlException("Invalid delta, couldn't submit.");
    }

    if (!serverDeltas.first().version.equals(clientVersion)) {
      LOG.warning("Mismatched hashes: expected: " + serverDeltas.first().version +
          " got: " + clientVersion);
      // Don't leak the hash to the client in the error message.
      throw new AccessControlException("Mismatched hashes at version number: " +
          clientVersion.getVersion());
    }

    List<WaveletOperation> serverOps = new ArrayList<WaveletOperation>();
    for (VersionedWaveletDelta d : serverDeltas) {
      serverOps.addAll(d.delta.getOperations());
    }
    List<WaveletOperation> transformedClientOps = transformOps(clientOps, serverOps);
    return transformedClientOps;
  }

  /**
   * Transforms the specified client operations against the specified server operations,
   * transforming the server operations in place but returning the transformed client operations
   * in a new list.
   *
   * @param clientOps may be unmodifiable
   * @param serverOps must support set(). Will contain the server ops transformed against client
   *        ops when this method returns
   * @return The result of transforming the client ops against the server ops.
   */
  private List<WaveletOperation> transformOps(List<WaveletOperation> clientOps,
      List<WaveletOperation> serverOps) throws OperationException {
    List<WaveletOperation> transformedClientOps = new ArrayList<WaveletOperation>();

    for (WaveletOperation c : clientOps) {
      for (int i = 0; i < serverOps.size(); i++) {
        WaveletOperation s = serverOps.get(i);
        OperationPair<WaveletOperation> pair;
        try {
          pair = Transform.transform(c, s);
        } catch(TransformException e) {
          throw new OperationException(e);
        }
        c = pair.clientOp();
        serverOps.set(i, pair.serverOp());
      }
      transformedClientOps.add(c);
    }
    return transformedClientOps;
  }

  /** Returns the canonical encoding of an applied delta. */
  private byte[] getCanonicalEncoding(ProtocolAppliedWaveletDelta delta) {
    ProtocolAppliedWaveletDelta.Builder builder =
        ProtocolAppliedWaveletDelta.newBuilder(delta);
    builder.setUnknownFields(UnknownFieldSet.getDefaultInstance());
    return builder.build().toByteArray();
  }


  @Override
  public NavigableSet<ProtocolAppliedWaveletDelta> requestHistory(
      ProtocolHashedVersion versionStart, ProtocolHashedVersion versionEnd)
      throws WaveletStateException {
    acquireReadLock();
    try {
      assertStateOk();
      // TODO: ### validate requested range.
      // TODO: #### make immutable.

      NavigableSet<ProtocolAppliedWaveletDelta> set = appliedDeltas.subSet(appliedDeltas.floor(emptyAppliedDeltaAtVersion(
          versionStart.getVersion())),true, emptyAppliedDeltaAtVersion(versionEnd.getVersion()),
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
}
