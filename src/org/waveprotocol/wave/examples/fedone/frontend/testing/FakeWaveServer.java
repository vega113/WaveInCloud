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

package org.waveprotocol.wave.examples.fedone.frontend.testing;

import static org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer.serialize;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.inject.internal.Nullable;

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.frontend.IndexWave;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.waveserver.federation.SubmitResultListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A fake single-user wave server which only echoes back submitted deltas and corresponding index
 * wave deltas.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
public class FakeWaveServer extends FakeClientFrontend {
  /** Fake application timestamp for confirming a successful submit. */
  private static final long APP_TIMESTAMP = 0;

  /** Fake document state for sending updates. */
  private static final ImmutableMap<String, BufferedDocOp> EMPTY_DOCUMENT_STATE = ImmutableMap.of();

  /**
   * A registry of the known wavelets, not including index wavelets. We use a list multimap to
   * preserve the wavelet order. When opening a wave, we want to send the conversation root deltas
   * first.
   */
  private final ListMultimap<WaveId, WaveletName> wavelets = ArrayListMultimap.create();

  /** A history of submitted deltas, per wavelet. Does not store generated index deltas. */
  private final ListMultimap<WaveletName, ProtocolWaveletDelta> deltas = ArrayListMultimap.create();

  /** The current versions of the user's wavelets, including index wavelets */
  private final Map<WaveletName, ProtocolHashedVersion> versions =
      new HashMap<WaveletName, ProtocolHashedVersion>();

  /** The user that is connected to this server */
  private ParticipantId user = null;


  @Override
  public void openRequest(ParticipantId participant, WaveId waveId,
      Set<String> waveletIdPrefixes, int maximumInitialWavelets, boolean snapshotsEnabled,
      final List<WaveClientRpc.WaveletVersion> knownWavelets, OpenListener openListener) {
    if (user == null) {
      user = participant;
    } else {
      Preconditions.checkArgument(participant.equals(user), "Unexpected user");
    }

    super.openRequest(participant, waveId, waveletIdPrefixes, maximumInitialWavelets,
        snapshotsEnabled, knownWavelets, openListener);

    // Send any deltas we have in this wave to the client, in the order we got them.
    // Note: the document state is ignored.
    // TODO(Michael): Can we use the knownWavelets here, and drop the wavelets collection?
    for (WaveletName waveletName : wavelets.get(waveId)) {
      waveletUpdate(waveletName, deltas.get(waveletName), versions.get(waveletName),
          EMPTY_DOCUMENT_STATE);
    }
  }

  @Override
  public void submitRequest(WaveletName waveletName, ProtocolWaveletDelta delta,
      @Nullable String channelId, SubmitResultListener listener) {
    Preconditions.checkArgument(!IndexWave.isIndexWave(waveletName.waveId),
        "Cannot modify index wave");

    super.submitRequest(waveletName, delta, channelId, listener);

    // If this is a new wavelet, add it to the registry.
    if (!versions.containsKey(waveletName)) {
      wavelets.put(waveletName.waveId, waveletName);
    }
    // Add the delta to the history and update the wavelet's version.
    deltas.put(waveletName, delta);
    final ProtocolHashedVersion resultingVersion = updateAndGetVersion(waveletName,
        delta.getOperationCount());

    // Confirm submit success.
    doSubmitSuccess(waveletName, resultingVersion, APP_TIMESTAMP);
    // Send an update echoing the submitted delta. Note: the document state is ignored.
    waveletUpdate(waveletName, Lists.newArrayList(delta), resultingVersion, EMPTY_DOCUMENT_STATE);
    // Send a corresponding update of the index wave.
    doIndexUpdate(waveletName, delta);
  }

  /**
   * Generate and send an update of the user's index wave based on the specified delta.
   *
   * @param waveletName name of wavelet where a change has been made.
   * @param delta the delta on the wavelet.
   */
  private void doIndexUpdate(WaveletName waveletName, ProtocolWaveletDelta delta) {
    // If the wavelet cannot be indexed, then the delta doesn't affect the index wave.
    if (!IndexWave.canBeIndexed(waveletName)) {
      return;
    }

    // TODO(Michael): Use IndexWave.createIndexDeltas instead of all this.

    // Go over the delta operations and extract only the add/remove participant ops that involve
    // our user. We do not update the index digests nor care about other users being added/removed.
    ProtocolWaveletDelta.Builder indexDelta = ProtocolWaveletDelta.newBuilder();
    for (ProtocolWaveletOperation op : delta.getOperationList()) {
      boolean copyOp = false;
      if (op.hasAddParticipant()) {
        copyOp |= (new ParticipantId(op.getAddParticipant()).equals(user));
      }
      if (op.hasRemoveParticipant()) {
        copyOp |= (new ParticipantId(op.getRemoveParticipant()).equals(user));
      }
      if (copyOp) {
        indexDelta.addOperation(ProtocolWaveletOperation.newBuilder(op).build());
      }
    }

    // If there is nothing to send, we're done.
    if (indexDelta.getOperationCount() == 0) {
      return;
    }

    // Find the index wavelet name and version. Update the version.
    WaveletName indexWaveletName = IndexWave.indexWaveletNameFor(waveletName.waveId);
    ProtocolHashedVersion resultingVersion = updateAndGetVersion(indexWaveletName,
        indexDelta.getOperationCount());

    // Finish constructing the index wavelet delta and send it to the client.
    indexDelta.setAuthor(delta.getAuthor());
    indexDelta.setHashedVersion(resultingVersion);
    waveletUpdate(indexWaveletName, Lists.newArrayList(indexDelta.build()), resultingVersion,
        EMPTY_DOCUMENT_STATE);
  }

  /**
   * Updates and returns the version of a given wavelet.
   *
   * @param waveletName of the wavelet whose version to update.
   * @param operationsCount applied to the wavelet.
   * @return the new hashed version of the wavelet.
   */
  private ProtocolHashedVersion updateAndGetVersion(WaveletName waveletName, int operationsCount) {
    // Get the current version.
    ProtocolHashedVersion version = versions.get(waveletName);

    // Calculate the new version.
    if (version != null) {
      version = serialize(HashedVersion.unsigned(version.getVersion() + operationsCount));
    } else {
      version = serialize(HashedVersion.unsigned(operationsCount));
    }

    // Store and return the new version.
    versions.put(waveletName, version);
    return version;
  }
}
