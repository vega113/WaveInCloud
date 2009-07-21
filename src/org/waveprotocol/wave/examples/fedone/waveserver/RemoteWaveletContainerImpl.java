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

import com.google.common.collect.ImmutableList;
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
  public DeltaSequence update(List<ProtocolAppliedWaveletDelta> appliedDeltas,
      final String domain, WaveletFederationProvider federationProvider)
      throws WaveServerException {
    acquireWriteLock();
    try {
      assertStateOkOrLoading();
      List<ProtocolWaveletDelta> result = new LinkedList<ProtocolWaveletDelta>();
      ProtocolHashedVersion expectedVersion =
          WaveletOperationSerializer.serialize(currentVersion);
      // Insert all available deltas into pendingDeltas.
      for (ProtocolAppliedWaveletDelta appliedDelta : appliedDeltas) {
        LOG.info("Delta incoming: " + appliedDelta);
        pendingDeltas.add(appliedDelta);
      }

      // Traverse pendingDeltas while we have any to process.
      while (pendingDeltas.size() > 0) {
        ProtocolAppliedWaveletDelta appliedDelta = pendingDeltas.first();
        ProtocolHashedVersion appliedAt = appliedDelta.getHashedVersionAppliedAt();
        // We don't have the right version - fall out, set up callback.
        if (appliedAt.getVersion() > expectedVersion.getVersion()) {
          // TODO: callback logic
          if (federationProvider != null) {
            LOG.info("Missing history from " + expectedVersion.getVersion() + "-"
                + appliedAt.getVersion() + ", requesting from upstream for " + waveletName);
            federationProvider.requestHistory(waveletName, domain, expectedVersion, appliedAt, -1,
                new HistoryResponseListener() {
                  @Override
                  public void onFailure(String errorMessage) {
                    LOG.severe("Callback failure: " + errorMessage);
                  }

                  @Override
                  public void onSuccess(Set<ProtocolAppliedWaveletDelta> deltaSet,
                      long lastCommittedVersion, long versionTruncatedAt) {
                    // Don't let this call instantiate another callback - pass
                    // null as the provider.
                    LOG.info("Got response callback: " + waveletName + ", lcv "
                        + lastCommittedVersion + " sizeof(deltaSet) = " + deltaSet.size());
                    try {
                      //update(ImmutableList.copyOf(deltaSet), domain, null);
                      acquireWriteLock();
                      // NB. This won't trigger an update until an up-to-date delta is received
                      for (ProtocolAppliedWaveletDelta appliedDelta : deltaSet) {
                        LOG.info("Delta incoming from remote: " + appliedDelta);
                        pendingDeltas.add(appliedDelta);
                      }
                    } finally {
                      releaseWriteLock();
                    }
                    // TODO: Only one requestHistory should be
                    // pending at any one time. We should derive a new one
                    // whenever the active one is finished, based on the current
                    // state of pendingDeltas.
                  }
                });
          }
          break;
        }

        // Is this delta at the correct version to be applied?
        if (appliedAt.getVersion() == expectedVersion.getVersion()) {
          LOG.info("RemoteWaveletContainer found delta exactly where we need: " + appliedAt);
          try {
            DeltaApplicationResult applicationResult =
                transformAndApplyDelta(appliedDelta.getSignedOriginalDelta());
            long opsApplied =
                applicationResult.getHashedVersionAfterApplication().getVersion()
                    - expectedVersion.getVersion();
            if (opsApplied != appliedDelta.getOperationsApplied()) {
              throw new OperationException("Operations applied here do not match the authoritative"
                  + " server claim (got " + opsApplied + ", expected "
                  + appliedDelta.getOperationsApplied() + ".");
            }
            // Add transformed result to return list.
            result.add(applicationResult.getDelta());
            LOG.info("Applied delta: " + appliedDelta);
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
        }
        pendingDeltas.remove(appliedDelta);
      }
      DeltaSequence deltaSequence = new DeltaSequence(result, expectedVersion);
      LOG.info("Returning contiguous block: " + deltaSequence);
      return deltaSequence;
    } finally {
      releaseWriteLock();
    }
  }
}
