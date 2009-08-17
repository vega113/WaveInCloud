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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationProvider.HistoryResponseListener;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.protocol.common.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.protocol.common.ProtocolHashedVersion;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;

/**
 * Remote wavelets differ from local ones in that deltas are not submitted for OT,
 * rather they are updated when a remote wave service provider has applied and sent
 * a delta.
 *
 *
 */
class RemoteWaveletContainerImpl extends WaveletContainerImpl implements
    RemoteWaveletContainer {
  private static final Log LOG = Log.get(RemoteWaveletContainerImpl.class);

  /**
   * Stores all pending deltas for this wavelet, whos insertions would cause
   * discontinuous blocks of deltas. This must only be accessed under writeLock.
   */
  private final NavigableSet<ByteStringMessage<ProtocolAppliedWaveletDelta>> pendingDeltas =
    Sets.newTreeSet(appliedDeltaComparator);

  /**
   * Create a new RemoteWaveletContainerImpl. Just pass through to the parent
   * constructor.
   */
  public RemoteWaveletContainerImpl(WaveletName waveletName) {
    super(waveletName);
    state = State.LOADING;
  }

  /** Convenience method to assert state. */
  protected void assertStateOkOrLoading() throws WaveletStateException {
    if (state != State.LOADING) {
      assertStateOk();
    }
  }

  @Override
  public boolean committed(ProtocolHashedVersion hashedVersion) throws WaveletStateException {
    acquireWriteLock();
    try {
      assertStateOkOrLoading();
      lastCommittedVersion = hashedVersion;

      // Pass to clients iff our known version is here or greater.
      return currentVersion.getVersion() >= hashedVersion.getVersion();
    } finally {
      releaseWriteLock();
    }
  }

  @Override
  public void update(List<ByteStringMessage<ProtocolAppliedWaveletDelta>> appliedDeltas,
      final String domain, WaveletFederationProvider federationProvider,
      final RemoteWaveletDeltaCallback deltaCallback) throws WaveServerException {
    acquireWriteLock();
    try {
      assertStateOkOrLoading();
      List<ProtocolWaveletDelta> result = new LinkedList<ProtocolWaveletDelta>();
      ProtocolHashedVersion expectedVersion =
          WaveletOperationSerializer.serialize(currentVersion);
      boolean haveRequestedHistory = false;

      // Insert all available deltas into pendingDeltas.
      for (ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta : appliedDeltas) {
        LOG.info("Delta incoming: " + appliedDelta);
        pendingDeltas.add(appliedDelta);
      }

      // Traverse pendingDeltas while we have any to process.
      while (pendingDeltas.size() > 0) {
        ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta = pendingDeltas.first();
        ProtocolHashedVersion appliedAt;
        try {
          appliedAt = getVersionAppliedAt(appliedDelta.getMessage());
        } catch (InvalidProtocolBufferException e) {
          setState(State.CORRUPTED);
          throw new WaveServerException(
              "Authoritative server sent delta with badly formed original wavelet delta", e);
        }

        // If we don't have the right version it implies there is a history we need, so set up a
        // callback to request it and fall out of this update
        if (appliedAt.getVersion() > expectedVersion.getVersion()) {
          LOG.info("Missing history from " + expectedVersion.getVersion() + "-"
              + appliedAt.getVersion() + ", requesting from upstream for " + waveletName);

          if (federationProvider != null) {
            // TODO: only one request history should be pending at any one time?
            // We should derive a new one whenever the active one is finished,
            // based on the current state of pendingDeltas.
            federationProvider.requestHistory(waveletName, domain, expectedVersion, appliedAt, -1,
                new HistoryResponseListener() {
                  @Override
                  public void onFailure(String errorMessage) {
                    LOG.severe("Callback failure: " + errorMessage);
                  }

                  @Override
                  public void onSuccess(List<ByteString> deltaList,
                      long lastCommittedVersion, long versionTruncatedAt) {
                    LOG.info("Got response callback: " + waveletName + ", lcv "
                        + lastCommittedVersion + " sizeof(deltaSet) = " + deltaList.size());

                    // Need to turn the ByteStrings in to a nice representation... sticky
                    List<ByteStringMessage<ProtocolAppliedWaveletDelta>> canonicalDeltaList =
                        Lists.newArrayList();
                    for (ByteString newAppliedDelta : deltaList) {
                      try {
                        ByteStringMessage<ProtocolAppliedWaveletDelta> canonicalDelta =
                            ByteStringMessage.from(
                                ProtocolAppliedWaveletDelta.getDefaultInstance(), newAppliedDelta);
                        LOG.info("Delta incoming from history: " + canonicalDelta);
                        canonicalDeltaList.add(canonicalDelta);
                      } catch (InvalidProtocolBufferException e) {
                        LOG.warning("Invalid protocol buffer when requesting history!");
                        break;
                      }
                    }

                    // Try updating again with the new set of deltas, with a null federation
                    // provider since we shouldn't need to reach here again
                    try {
                      update(canonicalDeltaList, domain, null, deltaCallback);
                    } catch (WaveServerException e) {
                      // TODO: deal with this
                      LOG.severe("Exception when updating from history", e);
                    }
                  }
                });
            haveRequestedHistory = true;
          } else {
            LOG.severe("History request resulted in non-contiguous deltas!");
          }
          break;
        }

        // This delta is at the correct (current) version - apply it.
        if (appliedAt.getVersion() == expectedVersion.getVersion()) {
          // Confirm that the applied at hash matches the expected hash.
          if (!appliedAt.equals(expectedVersion)) {
            state = State.CORRUPTED;
            throw new WaveServerException("Incoming delta applied at version "
                + appliedAt.getVersion() + " is not applied to the correct hash");
          }

          LOG.info("Applying delta for version " + appliedAt.getVersion());
          try {
            DeltaApplicationResult applicationResult = transformAndApplyRemoteDelta(appliedDelta);
            long opsApplied = applicationResult.getHashedVersionAfterApplication().getVersion()
                    - expectedVersion.getVersion();
            if (opsApplied != appliedDelta.getMessage().getOperationsApplied()) {
              throw new OperationException("Operations applied here do not match the authoritative"
                  + " server claim (got " + opsApplied + ", expected "
                  + appliedDelta.getMessage().getOperationsApplied() + ".");
            }
            // Add transformed result to return list.
            result.add(applicationResult.getDelta());
            LOG.fine("Applied delta: " + appliedDelta);
          } catch (OperationException e) {
            state = State.CORRUPTED;
            throw new WaveServerException("Couldn't apply authoritative delta", e);
          } catch (InvalidProtocolBufferException e) {
            state = State.CORRUPTED;
            throw new WaveServerException("Couldn't apply authoritative delta", e);
          } catch (InvalidHashException e) {
            state = State.CORRUPTED;
            throw new WaveServerException("Couldn't apply authoritative delta", e);
          }

          // This is the version 0 case - now we have a valid wavelet!
          if (state == State.LOADING) {
            state = State.OK;
          }

          // TODO: does waveletData update?
          expectedVersion = WaveletOperationSerializer.serialize(currentVersion);
        } else {
          LOG.warning("Got delta from the past: " + appliedDelta);
        }

        pendingDeltas.remove(appliedDelta);
      }

      if (!haveRequestedHistory) {
        DeltaSequence deltaSequence = new DeltaSequence(result, expectedVersion);
        if (LOG.isFineLoggable()) {
          LOG.fine("Returning contiguous block: " + deltaSequence);
        }
        deltaCallback.ready(deltaSequence);
      } else if (!result.isEmpty()) {
        LOG.severe("History requested but non-empty result, non-contiguous deltas?");
      } else {
        LOG.info("History requested, ignoring callback");
      }
    } finally {
      releaseWriteLock();
    }
  }

  /**
   * Apply a canonical applied delta to a remote wavelet. This assumes the
   * caller has validated that the delta is at the correct version and can be
   * applied to the wavelet. Must be called with writelock held.
   *
   * @param appliedDelta that is to be applied to the wavelet in its canonical form
   * @return transformed operations are applied to this delta
   * @throws AccessControlException if the supplied Delta's historyHash does not
   *         match the canonical history.
   */
  private DeltaApplicationResult transformAndApplyRemoteDelta(
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta) throws OperationException,
      AccessControlException, InvalidHashException, InvalidProtocolBufferException {

    // The canonical hashed version should actually match the currentVersion at this point, since
    // the caller of transformAndApply delta will have made sure the applied deltas are ordered
    HashedVersion canonicalHashedVersion =
      WaveletOperationSerializer.deserialize(getVersionAppliedAt(appliedDelta.getMessage()));
    if (!canonicalHashedVersion.equals(currentVersion)) {
      throw new IllegalStateException("Applied delta does not apply at current version");
    }

    // Extract the canonical wavelet delta
    ByteStringMessage<ProtocolWaveletDelta> protocolDelta = ByteStringMessage.from(
        ProtocolWaveletDelta.getDefaultInstance(),
        appliedDelta.getMessage().getSignedOriginalDelta().getDelta());
    WaveletDelta delta = WaveletOperationSerializer.deserialize(protocolDelta.getMessage()).first;

    // Transform operations against the current version
    List<WaveletOperation> transformedOps = maybeTransformSubmittedDelta(delta, currentVersion);
    if (transformedOps == null) {
      // As a sanity check, the hash from the applied delta should NOT be set (an optimisation, but
      // part of the protocol).
      if (appliedDelta.getMessage().hasHashedVersionAppliedAt()) {
        LOG.warning("Hashes are the same but applied delta has hashed_version_applied_at");
        // TODO: re-enable this exception for version 0.3 of the spec
//        throw new InvalidHashException("Applied delta and its contained delta have same hash");
      }
      transformedOps = delta.getOperations();
    }

    // Apply operations.  These shouldn't fail since they're the authoritative versions, so if they
    // do then the wavelet is corrupted (and the caller of this method will sort it out).
    int opsApplied = applyWaveletOperations(transformedOps);
    if (opsApplied != transformedOps.size()) {
      throw new OperationException(opsApplied + "/" + transformedOps.size() + " ops were applied");
    }

    return commitAppliedDelta(appliedDelta, new WaveletDelta(delta.getAuthor(), transformedOps));
  }
}
