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

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.waveserver.ClientFrontend.OpenListener;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;

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
 * None of the methods in this class accept null arguments.
 *
 * Note: Because this class has several maps keyed by waveId or waveletName,
 * we could potentially replace it with a WaveParticipant class that manages
 * all wavelets of a single participant on a particular wave.
 *
 *
 */
final class UserManager {
  /**
   * Result of a {@link #subscribe(WaveId, Set, OpenListener)} request.
   * Stores a set of waveletIdPrefixes and the listener to inform of changes
   * to any wavelet matching one of the prefixes.
   *
   * Each subscription belongs to a particular waveId (the waveId is not
   * stored as part of the subscription).
   */
  private static class Subscription {
    private final List<String> waveletIdPrefixes;
    private final OpenListener openListener;

    public Subscription(Set<String> waveletIdPrefixes, OpenListener openListener) {
      Preconditions.checkNotNull(waveletIdPrefixes);
      Preconditions.checkNotNull(openListener);
      this.waveletIdPrefixes = ImmutableList.copyOf(waveletIdPrefixes);
      this.openListener = openListener;
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
  synchronized List<OpenListener> matchSubscriptions(WaveletName waveletName) {
    List<OpenListener> result = Lists.newArrayList();
    for (Subscription subscription : subscriptions.get(waveletName.waveId)) {
      if (subscription.matches(waveletName.waveletId)) {
        result.add(subscription.openListener);
      }
    }
    return result;
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
  synchronized void onUpdate(WaveletName waveletName, DeltaSequence deltas) {
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

    List<OpenListener> listeners = matchSubscriptions(waveletName);
    for (OpenListener listener : listeners) {
      try {
        listener.onUpdate(waveletName, null, deltas, deltas.getEndVersion(), null);
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
  void onCommit(WaveletName waveletName, ProtocolHashedVersion version) {
    Preconditions.checkNotNull(waveletName);
    Preconditions.checkNotNull(version);
    if (!isParticipant(waveletName)) {
      throw new IllegalStateException("Not a participant of wavelet " + waveletName);
    }
    List<ProtocolWaveletDelta> emptyList = Collections.emptyList();
    List<OpenListener> listeners = matchSubscriptions(waveletName);
    for (OpenListener listener : listeners) {
      listener.onUpdate(waveletName, null, emptyList, null, version);
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
      WaveId waveId, Set<String> waveletIdPrefixes, OpenListener listener) {
    Preconditions.checkNotNull(waveId);
    Subscription subscription = new Subscription(waveletIdPrefixes, listener);
    subscriptions.put(waveId, subscription);
    Set<WaveletId> result = Sets.newHashSet();
    for (WaveletId waveletId : getWaveletIds(waveId)) {
      if (subscription.matches(waveletId)) {
        result.add(waveletId);
      }
    }
    return result;
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
        WaveletOperationSerializer.serialize(HashedVersion.versionZero(waveletName)));
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
