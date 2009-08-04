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

import org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationProvider.HistoryResponseListener;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.protocol.common.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.protocol.common.ProtocolHashedVersion;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

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
  private final NavigableSet<ProtocolAppliedWaveletDelta> pendingDeltas =
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
  public void update(List<ProtocolAppliedWaveletDelta> appliedDeltas, final String domain,
      WaveletFederationProvider federationProvider, final RemoteWaveletDeltaCallback deltaCallback)
      throws WaveServerException {
    acquireWriteLock();
    try {
      assertStateOkOrLoading();
      List<ProtocolWaveletDelta> result = new LinkedList<ProtocolWaveletDelta>();
      ProtocolHashedVersion expectedVersion =
          WaveletOperationSerializer.serialize(currentVersion);
      boolean haveRequestedHistory = false;

      // Insert all available deltas into pendingDeltas.
      for (ProtocolAppliedWaveletDelta appliedDelta : appliedDeltas) {
        LOG.info("Delta incoming: " + appliedDelta);
        pendingDeltas.add(appliedDelta);
      }

      // Traverse pendingDeltas while we have any to process.
      while (pendingDeltas.size() > 0) {
        ProtocolAppliedWaveletDelta appliedDelta = pendingDeltas.first();
        ProtocolHashedVersion appliedAt = appliedDelta.getHashedVersionAppliedAt();

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
                  public void onSuccess(Set<ProtocolAppliedWaveletDelta> deltaSet,
                      long lastCommittedVersion, long versionTruncatedAt) {
                    LOG.info("Got response callback: " + waveletName + ", lcv "
                        + lastCommittedVersion + " sizeof(deltaSet) = " + deltaSet.size());

                    // Try updating again with the new set of deltas, with a null federation
                    // provider since we shouldn't need to reach here again
                    List<ProtocolAppliedWaveletDelta> deltaList = Lists.newArrayList();
                    for (ProtocolAppliedWaveletDelta newAppliedDelta : deltaSet) {
                      LOG.info("Delta incoming from history: " + newAppliedDelta);
                      deltaList.add(newAppliedDelta);
                    }

                    try {
                      update(deltaList, domain, null, deltaCallback);
                    } catch (WaveServerException e) {
                      LOG.severe("Exception when re-running update with history: " + e);
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
            DeltaApplicationResult applicationResult =
                transformAndApplyDelta(appliedDelta.getSignedOriginalDelta(),
                    appliedDelta.getApplicationTimestamp());
            long opsApplied = applicationResult.getHashedVersionAfterApplication().getVersion()
                    - expectedVersion.getVersion();
            if (opsApplied != appliedDelta.getOperationsApplied()) {
              throw new OperationException("Operations applied here do not match the authoritative"
                  + " server claim (got " + opsApplied + ", expected "
                  + appliedDelta.getOperationsApplied() + ".");
            }
            // Add transformed result to return list.
            result.add(applicationResult.getDelta());
            LOG.fine("Applied delta: " + appliedDelta);
          } catch (OperationException e) {
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
}
