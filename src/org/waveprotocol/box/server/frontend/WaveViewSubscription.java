// Copyright 2010 Google Inc. All Rights Reserved.

package org.waveprotocol.box.server.frontend;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A client's subscription to a wave view.
 *
 * @author anorth@google.com (Alex North)
 */
final class WaveViewSubscription {

  private static final Log LOG = Log.get(WaveViewSubscription.class);

  private final WaveId waveId;
  private final IdFilter waveletIdFilter;
  private final ClientFrontend.OpenListener openListener;
  private final String channelId;
  // Successfully submitted versions for which we haven't yet seen the update
  private final HashMultimap<WaveletId, Long> submittedVersions = HashMultimap.create();
  // Wavelets with outstanding submits
  private final Set<WaveletId> outstandingSubmits = Sets.newHashSet();
  // Current version of each wavelet
  private final Map<WaveletId, HashedVersion> currentVersions = Maps.newHashMap();

  public WaveViewSubscription(WaveId waveId, IdFilter waveletIdFilter, String channelId,
      ClientFrontend.OpenListener openListener) {
    Preconditions.checkNotNull(waveId, "null wave id");
    Preconditions.checkNotNull(waveletIdFilter, "null filter");
    Preconditions.checkNotNull(openListener, "null listener");
    Preconditions.checkNotNull(channelId, "null channel id");

    this.waveId = waveId;
    this.waveletIdFilter = waveletIdFilter;
    this.channelId = channelId;
    this.openListener = openListener;
  }

  public WaveId getWaveId() {
    return waveId;
  }

  public ClientFrontend.OpenListener getOpenListener() {
    return openListener;
  }

  public String getChannelId() {
    return channelId;
  }

  /**
   * Checks whether the subscription includes a wavelet.
   */
  public boolean includes(WaveletId waveletId) {
    return IdFilter.accepts(waveletIdFilter, waveletId);
  }

  /** This client sent a submit request */
  public synchronized void submitRequest(WaveletName waveletName) {
    // A given client can only have one outstanding submit per wavelet.
    Preconditions.checkState(!outstandingSubmits.contains(waveletName.waveletId),
        "Received overlapping submit requests to subscription %s", this);
    LOG.info("Submit oustandinding on channel " + channelId);
    outstandingSubmits.add(waveletName.waveletId);
  }

  /**
   * A submit response for the given wavelet and version has been sent to this
   * client.
   */
  public synchronized void submitResponse(WaveletName waveletName, HashedVersion version) {
    Preconditions.checkNotNull(version, "Null delta application version");
    WaveletId waveletId = waveletName.waveletId;
    submittedVersions.put(waveletId, version.getVersion());
    outstandingSubmits.remove(waveletId);
    LOG.info("Submit resolved on channel " + channelId);
  }

  /**
   * Sends an update for this subscription (if appropriate).
   *
   * If the update contains a delta for a wavelet where the delta is actually
   * from this client, don't send that delta. If there's outstanding submits
   * waiting, just queue the updates.
   */
  public void onUpdate(final WaveletName waveletName,
      @Nullable final WaveletSnapshotAndVersion snapshot, final DeltaSequence deltas,
      @Nullable final HashedVersion endVersion,
      @Nullable final HashedVersion committedVersion, final boolean hasMarker) {
    checkUpdateVersion(waveletName, snapshot, deltas);
    if (deltas.isEmpty()) {
      openListener.onUpdate(waveletName, snapshot, deltas, endVersion, committedVersion, hasMarker,
          channelId);
    }
    WaveletId waveletId = waveletName.waveletId;
    List<CoreWaveletDelta> filteredDeltas;
    if (!submittedVersions.isEmpty() && !submittedVersions.get(waveletId).isEmpty()) {
      // Walk through the deltas, removing any that are from this client.
      filteredDeltas = Lists.newArrayList();
      Set<Long> mySubmits = submittedVersions.get(waveletId);

      for (CoreWaveletDelta delta : deltas) {
        long deltaEndVersion = delta.getTargetVersion().getVersion() + delta.getOperations().size();
        if (mySubmits.contains(deltaEndVersion)) {
          submittedVersions.remove(waveletId, deltaEndVersion);
        } else {
          filteredDeltas.add(delta);
        }
      }
    } else {
      filteredDeltas = deltas;
    }
    if (!filteredDeltas.isEmpty()) {
      openListener.onUpdate(waveletName, snapshot, filteredDeltas, endVersion, committedVersion,
          hasMarker, channelId);
    }
  }

  /**
   * Checks the update targets the next expected version.
   */
  private void checkUpdateVersion(WaveletName waveletName, WaveletSnapshotAndVersion snapshot,
      DeltaSequence deltas) {
    if (snapshot != null) {
      Preconditions.checkArgument(deltas.isEmpty(), "Unexpected deltas with snapshot for %s",
          waveletName);
      currentVersions.put(waveletName.waveletId,
          CoreWaveletOperationSerializer.deserialize(snapshot.snapshot.getVersion()));
    } else if (!deltas.isEmpty()) {
      if (currentVersions.containsKey(waveletName.waveletId)) {
        HashedVersion expectedVersion = currentVersions.get(waveletName.waveletId);
        HashedVersion targetVersion = deltas.getStartVersion();
        Preconditions.checkState(targetVersion.equals(expectedVersion),
            "Subscription expected delta for %s targetting %s, was %s", waveletName,
            expectedVersion, targetVersion);
      }
      HashedVersion nextExpectedVersion = deltas.getEndVersion();
      currentVersions.put(waveletName.waveletId, nextExpectedVersion);
    }
  }

  @Override
  public String toString() {
    return "[WaveViewSubscription wave: " + waveId + ", channel: " + channelId + "]";
  }
}
