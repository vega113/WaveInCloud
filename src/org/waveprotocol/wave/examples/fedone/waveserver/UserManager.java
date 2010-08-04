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
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.internal.Nullable;

import org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.waveserver.ClientFrontend.OpenListener;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-participant state (wavelets the user is subscribed to, and
 * active subscription channels).
 *
 * This class has no special knowledge of the Index Wave and treats
 * it no different from other waves.
 *
 * Note: Because this class has several maps keyed by waveId or waveletName,
 * we could potentially replace it with a WaveParticipant class that manages
 * all wavelets of a single participant on a particular wave.
 *
 */
final class UserManager {

  /**
   * Result of a {@link #subscribe(WaveId, Set, String, OpenListener)} request.
   * Stores a set of waveletIdPrefixes, channel_id and the listener to inform of changes
   * to any wavelet matching one of the prefixes.
   *
   * Each subscription belongs to a particular waveId (the waveId is not
   * stored as part of the subscription).
   */
  @VisibleForTesting
  static class Subscription {

    private final List<String> waveletIdPrefixes;
    private final OpenListener openListener;
    private String channelId;
    // Successfully submitted versions for which we haven't yet seen the update
    private final HashMultimap<WaveletName, Long> submittedVersions = HashMultimap.create();
    // While there are outstanding submits, this contains the list of updates that are blocked.
    private final ListMultimap<WaveletName, Runnable> queuedUpdates = LinkedListMultimap.create();
    // A set of outstanding submits by wavelet name
    private final Set<WaveletName> outstandingSubmits = Sets.newHashSet();

    public Subscription(Set<String> waveletIdPrefixes, String channelId,
        OpenListener openListener) {
      Preconditions.checkNotNull(waveletIdPrefixes);
      Preconditions.checkNotNull(openListener);
      Preconditions.checkNotNull(channelId);

      this.waveletIdPrefixes = ImmutableList.copyOf(waveletIdPrefixes);
      this.channelId = channelId;
      this.openListener = openListener;
    }

    /**
     * @return the waveletIdPrefixes
     */
    public List<String> getWaveletIdPrefixes() {
      return waveletIdPrefixes;
    }

    /**
     * @return the openListener
     */
    public OpenListener getOpenListener() {
      return openListener;
    }

    /**
     * @return the channelId
     */
    public String getChannelId() {
      return channelId;
    }

    /**
     * @return the submittedVersions
     */
    public HashMultimap<WaveletName, Long> getSubmittedVersions() {
      return submittedVersions;
    }

    /**
     * @return the queuedUpdates
     */
    public ListMultimap<WaveletName, Runnable> getQueuedUpdates() {
      return queuedUpdates;
    }

    /**
     * @return the outstandingSubmits
     */
    public Set<WaveletName> getOutstandingSubmits() {
      return outstandingSubmits;
    }

    /** This client sent a submit request */
    synchronized void submitRequest(WaveletName waveletName) {
      // A given client can only have one outstanding submit per wave.
      outstandingSubmits.add(waveletName);
    }

    /** A submit response for the given wavelet and version has been sent to this client */
    synchronized void submitResponse(WaveletName waveletName, ProtocolHashedVersion version) {
      if (version != null) {
        submittedVersions.put(waveletName, version.getVersion());
      }
      outstandingSubmits.remove(waveletName);
      final List<Runnable> updatesForWavelet = queuedUpdates.get(waveletName);
      while(!updatesForWavelet.isEmpty()) {
        Runnable runnable = updatesForWavelet.remove(0);
        runnable.run();
      }
    }

    /**
     * Sends an update for this subscription (if appropriate).
     *
     * If the update contains a delta for a wavelet where the delta is actually from this client,
     * don't send that delta. If there's outstanding submits waiting, just queue the updates.
     *
     * @param waveletName
     * @param snapshot
     * @param deltas
     * @param endVersion
     * @param committedVersion
     * @param hasMarker
     * @param channelId
     */
    void onUpdate(final WaveletName waveletName,
        @Nullable final WaveletSnapshotAndVersions snapshot,
        final List<ProtocolWaveletDelta> deltas, @Nullable final ProtocolHashedVersion endVersion,
        @Nullable final ProtocolHashedVersion committedVersion, final boolean hasMarker,
        final String channelId) {
      if (deltas.isEmpty()) {
        openListener.onUpdate(waveletName, snapshot, deltas, endVersion, committedVersion,
            hasMarker, channelId);
      }
      if (!outstandingSubmits.isEmpty()) {
        queuedUpdates.put(waveletName, new Runnable() {

          @Override
          public void run() {
            onUpdate(waveletName, snapshot, deltas, endVersion, committedVersion, hasMarker,
                channelId);
          }
        });
        return;
      }
      List<ProtocolWaveletDelta> newDeltas;

      if (!deltas.isEmpty() && !submittedVersions.isEmpty()
          && !submittedVersions.get(waveletName).isEmpty()) {
        // Walk through the deltas, removing any that are from this client.
        newDeltas = Lists.newArrayList();
        Set<Long> mySubmits = submittedVersions.get(waveletName);

        for (ProtocolWaveletDelta delta : deltas) {
          if (mySubmits.contains(delta.getPostHashedVersion().getVersion())) {
            submittedVersions.remove(waveletName, delta.getPostHashedVersion().getVersion());
          } else {
            newDeltas.add(delta);
          }
        }
      } else {
        newDeltas = deltas;
      }
      if (!newDeltas.isEmpty()) {
        openListener.onUpdate(waveletName, snapshot, newDeltas, endVersion, committedVersion,
            hasMarker, channelId);
      }
    }

    /**
     * Returns true if the listener should be informed of changes to the
     * specified wavelet.
     */
    boolean matches(WaveletId waveletId) {
      // TODO: Could be made more efficient with a trie
      List<OpenListener> result = Lists.newArrayList();
      String waveletIdStr = waveletId.serialise();

      for (String prefix : waveletIdPrefixes) {
        if (waveletIdStr.startsWith(prefix)) {
          return true;
        }
      }
      return false;
    }
  }

  private final ListMultimap<WaveId, Subscription> subscriptions;

  /** Wavelets that this user is a participant of. */
  private final HashMultimap<WaveId, WaveletId> waveletIds;

  /**
   * The current version of the specified wavelet, as per the deltas received
   * for it so far. This contains a waveletName if and only if we are a
   * participant on that wavelet. Invariant: At the start and end of
   * onUpdate(), the listeners from all active subscriptions have received
   * the deltas up to this version.
   */
  private final Map<WaveletName, ProtocolHashedVersion> currentVersion;

  /**
   * Tracks versions of wavelets submitted by client, in order to suppress the wavelet
   * updates for that client. The key is channelId, wavelet name.
   */
  private final Set<Pair<String, WaveletName>> outstandingSubmits = Sets.newHashSet();
  private final Map<Pair<String, WaveletName>, Set<Long>> submittedVersions = Maps.newHashMap();

  UserManager() {
    this.subscriptions = LinkedListMultimap.create();
    this.waveletIds = HashMultimap.create();
    this.currentVersion = Maps.newHashMap();
  }

  /** Whether this user is a participant on the specified wavelet. */
  synchronized boolean isParticipant(WaveletName waveletName) {
    return currentVersion.containsKey(waveletName);
    // Alternatively, could do:
    //return waveletIds.get(waveletName.waveId).contains(waveletName.waveletId);
  }

  /** The listeners interested in the specified wavelet. */
  @VisibleForTesting
  synchronized List<Subscription> matchSubscriptions(WaveletName waveletName) {
    List<Subscription> result = Lists.newArrayList();
    for (Subscription subscription : subscriptions.get(waveletName.waveId)) {
      if (subscription.matches(waveletName.waveletId)) {
        result.add(subscription);
      }
    }
    return result;
  }

  /** Returns the subscription (if it exists) for a given wavelet and channel */
  synchronized Subscription findSubscription(WaveletName waveletName, String channelId) {
    for (Subscription subscription : subscriptions.get(waveletName.waveId)) {
      if (subscription.matches(waveletName.waveletId)) {
        if (subscription.channelId.equals(channelId)) {
          return subscription;
        }
      }
    }
    return null;
  }

  synchronized ProtocolHashedVersion getWaveletVersion(WaveletName waveletName) {
    Preconditions.checkArgument(isParticipant(waveletName), "Not a participant of " + waveletName);
    return currentVersion.get(waveletName);
  }

  /**
   * Receives additional deltas for the specified wavelet, of which we
   * must be a participant.
   *
   * @throws NullPointerException if waveletName or deltas is null
   * @throws IllegalStateException if we're not a participant of the wavelet
   * @throws IllegalArgumentException if the version numbering of the deltas is
   *         not properly contiguous from another or from deltas we previously
   *         received for this delta.
   */
  synchronized void onUpdate(WaveletName waveletName, DeltaSequence deltas, String channelId) {
    Preconditions.checkNotNull(waveletName);
    if (deltas.isEmpty()) {
      return;
    }
    if (!isParticipant(waveletName)) {
      throw new IllegalStateException("Not a participant of wavelet " + waveletName);
    }
    long version = deltas.getStartVersion().getVersion();
    long expectedVersion = currentVersion.get(waveletName).getVersion();
    Preconditions.checkArgument(expectedVersion == version,
        "Expected startVersion " + expectedVersion + ", got " + version);
    currentVersion.put(waveletName, deltas.getEndVersion());

    List<Subscription> subscriptions = matchSubscriptions(waveletName);
    for (Subscription subscription : subscriptions) {
      try {
        // Last 3 args are committedVersion, hasMarker and channelId.
        subscription.onUpdate(waveletName, null, deltas, deltas.getEndVersion(), null, false, null);
      } catch (IllegalStateException e) {
        // TODO: remove the listener
      }
    }
  }

  /**
   * Receives notification that the specified wavelet has been committed at the
   * specified version.
   *
   * @throws NullPointerException if waveletName or version is null
   * @throws IllegalStateException if we're not a participant of the wavelet
   */
  void onCommit(WaveletName waveletName, ProtocolHashedVersion version, String channelId) {
    Preconditions.checkNotNull(waveletName);
    Preconditions.checkNotNull(version);
    if (!isParticipant(waveletName)) {
      throw new IllegalStateException("Not a participant of wavelet " + waveletName);
    }
    List<ProtocolWaveletDelta> emptyList = Collections.emptyList();
    // TODO(arb): do we send commits back to the original client??
    List<Subscription> listeners = matchSubscriptions(waveletName);
    for (Subscription listener : listeners) {
      listener.onUpdate(waveletName, null, emptyList, null, version, false, null);
    }
  }

  /**
   * Subscribes to updates from the specified waveId and waveletIds with
   * the specified prefixes, using the specified listener to receive updates.
   *
   * @return All subscribed waveletIds that are of interest to the listener.
   *         The caller must ensure that the listener gets deltas 0 (inclusive)
   *         through getWaveletVersion(WaveletName.of(waveId, waveletId))
   *         exclusive before onUpdate() is next called on this UserManager.
   *         This is to ensure that the listener catches up with all other
   *         listeners on those wavelets before further deltas are broadcast
   *         to all listeners.
   */
  synchronized Set<WaveletId> subscribe(
      WaveId waveId, Set<String> waveletIdPrefixes, String channelId, OpenListener listener) {
    Preconditions.checkNotNull(waveId);
    Subscription subscription = new Subscription(waveletIdPrefixes, channelId, listener);
    subscriptions.put(waveId, subscription);
    Set<WaveletId> result = Sets.newHashSet();
    for (WaveletId waveletId : getWaveletIds(waveId)) {
      // We don't pass the channel_id for this case, because we _do_ want the subscriptions for
      // this channel.
      if (subscription.matches(waveletId)) {
        result.add(waveletId);
      }
    }
    return result;
  }

  /**
   * Tell the user manager that we have a submit request outstanding.
   * While a submit request is outstanding, all wavelet updates are queued.
   *
   * @param channelId the channel identifying the specific client
   * @param waveletName the name of the wavelet
   */
  public void submitRequest(String channelId, WaveletName waveletName) {
    Subscription subscription = findSubscription(waveletName, channelId);
    if (subscription != null) {
      subscription.submitRequest(waveletName);
    }
  }

  /**
   * Signal the user manager that a submit response has been sent for the given wavelet and
   * version.
   * Any pending wavelet updates will be sent.
   * A matching wavelet update for the given wavelet name and version will be discarded.
   * @param channelId the channel identifying the specific client
   * @param waveletName the name of the wavelet
   * @param hashedVersionAfterApplication the version of the wavelet in the response (or null if the
   *                                      submit request failed.
   */
  public void submitResponse(String channelId, WaveletName waveletName,
      ProtocolHashedVersion hashedVersionAfterApplication) {
    Subscription subscription = findSubscription(waveletName, channelId);
    if (subscription != null) {
      subscription.submitResponse(waveletName, hashedVersionAfterApplication);
    }
  }

  /**
   * Notifies that the user has been added to the specified wavelet.
   */
  synchronized void addWavelet(WaveletName waveletName) {
    if (isParticipant(waveletName)) {
      throw new IllegalStateException("Already a participant of " + waveletName);
    }
    waveletIds.get(waveletName.waveId).add(waveletName.waveletId);
    currentVersion.put(waveletName,
        CoreWaveletOperationSerializer.serialize(HashedVersion.versionZero(waveletName)));
  }

  /**
   * Notifies that the user has been removed from the specified wavelet
   * @param waveletName we were removed from
   */
  synchronized void removeWavelet(WaveletName waveletName) {
    if (!isParticipant(waveletName)) {
      throw new IllegalStateException("Not a participant of " + waveletName);
    }
    waveletIds.get(waveletName.waveId).remove(waveletName.waveletId);
    currentVersion.remove(waveletName);
  }

  synchronized Set<WaveletId> getWaveletIds(WaveId waveId) {
    Preconditions.checkNotNull(waveId);
    return Collections.unmodifiableSet(waveletIds.get(waveId));
  }
}
