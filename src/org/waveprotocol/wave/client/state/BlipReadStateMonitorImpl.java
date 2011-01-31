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
 */
package org.waveprotocol.wave.client.state;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.model.conversation.BlipIterators;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.IdentitySet;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Set;

/**
 * Monitors the conversation for unread blips and efficiently maintains a count
 * of them.
 */
public final class BlipReadStateMonitorImpl extends ObservableSupplementedWave.ListenerImpl
    implements BlipReadStateMonitor, ObservableConversation.Listener,
    ObservableConversationView.Listener {

  /**
   * Logging for state changes. Added June 2010; note that many log statements
   * output the current read/unread state, which is actually an O(n) operation
   * in the number of blips since the JS implementation IdentityMap counts all
   * entries (albeit very cheaply).
   */
  private static final DomLogger LOG = new DomLogger("blip-read-state");
  static {
    LOG.enableModuleBuffer(true);
  }

  private final IdentitySet<ConversationBlip> readBlips = CollectionUtils.createIdentitySet();
  private final IdentitySet<ConversationBlip> unreadBlips = CollectionUtils.createIdentitySet();
  private final IdentityMap<ConversationBlip, String> blipLogDescriptions =
      CollectionUtils.createIdentityMap();
  private final CopyOnWriteSet<BlipReadStateMonitor.Listener> listeners = CopyOnWriteSet.create();
  private final ObservableSupplementedWave supplementedWave;
  private final ObservableConversationView conversationView;
  private final WaveId waveId;
  private boolean haveCountedBlips = false;

  /**
   * @return a new BlipReadStateMonitor
   */
  public static BlipReadStateMonitorImpl create(WaveId waveId,
      ObservableSupplementedWave supplementedWave, ObservableConversationView conversationView) {
    BlipReadStateMonitorImpl monitor = new BlipReadStateMonitorImpl(waveId,
        supplementedWave, conversationView);
    monitor.init();
    return monitor;
  }

  private BlipReadStateMonitorImpl(WaveId waveId, ObservableSupplementedWave supplementedWave,
      ObservableConversationView conversationView) {
    Preconditions.checkNotNull(waveId, "waveId cannot be null");
    Preconditions.checkNotNull(supplementedWave, "supplementedWave cannot be null");
    Preconditions.checkNotNull(conversationView, "conversationView cannot be null");
    this.waveId = waveId;
    this.supplementedWave = supplementedWave;
    this.conversationView = conversationView;
  }

  private void init() {
    // Listen to existing conversations.
    for (ObservableConversation conversation : conversationView.getConversations()) {
      conversation.addListener(this);
    }

    // Listen for new conversations and supplement events.
    supplementedWave.addListener(this);
    conversationView.addListener(this);

    // Count the existing blips.  This will also set haveCountedBlips to true
    // and therefore isReady() to return true.
    countBlips();

    // Notify listeners of any read/unread state events they might have missed.
    notifyListeners();
  }

  //
  // Debugging (for DebugMenu).
  //

  public Set<String> debugGetReadBlips() {
    final ImmutableSet.Builder<String> readBlipsBuilder = ImmutableSet.builder();
    readBlips.each(new IdentitySet.Proc<ConversationBlip>() {
      @Override
      public void apply(ConversationBlip blip) {
        readBlipsBuilder.add(blip.getId());
      }
    });
    return readBlipsBuilder.build();
  }

  public Set<String> debugGetUnreadBlips() {
    final ImmutableSet.Builder<String> unreadBlipsBuilder = ImmutableSet.builder();
    unreadBlips.each(new IdentitySet.Proc<ConversationBlip>() {
      @Override
      public void apply(ConversationBlip blip) {
        unreadBlipsBuilder.add(blip.getId());
      }
    });
    return unreadBlipsBuilder.build();
  }

  //
  // BlipReadStateMonitor
  //

  @Override
  public int getReadCount() {
    return readBlips.countEntries();
  }

  @Override
  public int getUnreadCount() {
    return unreadBlips.countEntries();
  }

  @Override
  public boolean isReady() {
    return haveCountedBlips;
  }

  @Override
  public void addListener(BlipReadStateMonitor.Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(BlipReadStateMonitor.Listener listener) {
    listeners.remove(listener);
  }

  //
  // ObservableConversation.Listener
  //

  @Override
  public void onBlipAdded(ObservableConversationBlip blip) {
    logChange("added", blip);
    handleBlipAdded(blip);
  }

  private void handleBlipAdded(ObservableConversationBlip blip) {
    // Add this blip.
    updateOrInsertReadUnread(blip);

    // Add all replies.
    for (ObservableConversationThread replyThread : blip.getAllReplyThreads()) {
      handleThreadAdded(replyThread);
    }
  }

  @Override
  public void onBlipDeleted(ObservableConversationBlip blip) {
    logChange("deleted", blip);
    handleBlipRemoved(blip);
  }

  private void handleBlipRemoved(ObservableConversationBlip blip) {
    // Remove this blip.
    removeReadUnread(blip);

    // Remove all inline replies (non-inline replies will just be reanchored).
    for (ObservableConversationThread replyThread : blip.getAllReplyThreads()) {
      if (replyThread.isInline()) {
        handleThreadRemoved(replyThread);
      }
    }
  }

  @Override
  public void onThreadAdded(ObservableConversationThread thread) {
    handleThreadAdded(thread);
  }

  private void handleThreadAdded(ObservableConversationThread thread) {
    // Add all direct blips.  Descendant blips will be added recursively.
    for (ObservableConversationBlip blip : thread.getBlips()) {
      handleBlipAdded(blip);
    }
  }

  @Override
  public void onThreadDeleted(ObservableConversationThread thread) {
    handleThreadRemoved(thread);
  }

  private void handleThreadRemoved(ObservableConversationThread thread) {
    // Remove all direct blips.  Descendant blips will be removed recursively.
    for (ObservableConversationBlip blip : thread.getBlips()) {
      handleBlipRemoved(blip);
    }
  }

  //
  // ObservableConversationView.Listener
  //

  @Override
  public void onConversationAdded(ObservableConversation conversation) {
    conversation.addListener(this);
    handleThreadAdded(conversation.getRootThread());
  }

  @Override
  public void onConversationRemoved(ObservableConversation conversation) {
    conversation.removeListener(this);
    handleThreadRemoved(conversation.getRootThread());
  }

  //
  // ObservableSupplementedWave.Listener
  //

  @Override
  public void onMaybeBlipReadChanged(ObservableConversationBlip blip) {
    // We only care about blips that we already know about.
    if (readBlips.contains(blip) || unreadBlips.contains(blip)) {
      if (updateOrInsertReadUnread(blip)) {
        logChange("read changed", blip);
      }
    }
  }

  @Override
  public void onMaybeWaveletReadChanged() {
    countBlips();
    notifyListeners();
  }

  //
  // Helpers.
  //

  /**
   * Populates {@link #readBlips} and {@link #unreadBlips} by counting all blips.
   */
  private void countBlips() {
    readBlips.clear();
    unreadBlips.clear();

    for (Conversation conversation : conversationView.getConversations()) {
      for (ConversationBlip blip : BlipIterators.breadthFirst(conversation)) {
        if (supplementedWave.isUnread(blip)) {
          unreadBlips.add(blip);
        } else {
          readBlips.add(blip);
        }
      }
    }

    haveCountedBlips = true;
  }

  /**
   * Inserts the blip into the correct read/unread set and removes from the
   * other, and notifies listeners as needed.
   */
  private boolean updateOrInsertReadUnread(ConversationBlip blip) {
    boolean changed = false;
    if (isUnread(blip)) {
      if (readBlips.contains(blip)) {
        readBlips.remove(blip);
        changed = true;
      }
      if (!unreadBlips.contains(blip)) {
        unreadBlips.add(blip);
        changed = true;
      }
    } else {
      if (unreadBlips.contains(blip)) {
        unreadBlips.remove(blip);
        changed = true;
      }
      if (!readBlips.contains(blip)) {
        readBlips.add(blip);
        changed = true;
      }
    }
    if (changed) {
      notifyListeners();
    }
    return changed;
  }

  /**
   * Removes the blip from all possible locations in the read and unread set
   * and notifies listeners as needed.
   */
  private void removeReadUnread(ConversationBlip blip) {
    boolean changed = false;
    if (readBlips.contains(blip)) {
      readBlips.remove(blip);
      changed = true;
    }
    if (unreadBlips.contains(blip)) {
      unreadBlips.remove(blip);
      changed = true;
    }
    if (changed) {
      notifyListeners();
    }
  }

  /**
   * Determines whether the given blip is unread.
   */
  private boolean isUnread(ConversationBlip blip) {
    return supplementedWave.isUnread(blip);
  }

  /**
   * Notifies listeners of a change.
   */
  private void notifyListeners() {
    // countEntries() is O(n), don't recount every time.
    int readCount = getReadCount();
    int unreadCount = getUnreadCount();
    LOG.trace().log(waveId, ": notifying read/unread change ", readCount, "/", unreadCount);
    for (Listener listener : listeners) {
      listener.onReadStateChanged(readCount, unreadCount);
    }
  }

  /**
   * Log some action with the blip information and read/unread state.
   */
  private void logChange(String action, ConversationBlip blip) {
    LOG.trace().log(getBlipLogDescription(blip), ": ", action, " now ",
        getReadCount(), "/", getUnreadCount());
  }

  /**
   * Gets the log description for a blip and caches the result.
   */
  private String getBlipLogDescription(ConversationBlip blip) {
    if (!blipLogDescriptions.has(blip)) {
      blipLogDescriptions.put(blip, ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId) + "/"
          + blip.getConversation().getId() + "/" + blip.getId());
    }
    return blipLogDescriptions.get(blip);
  }

  @Override public void onBlipContributorAdded(ObservableConversationBlip blip,
      ParticipantId contributor) {}
  @Override public void onBlipContributorRemoved(ObservableConversationBlip blip,
      ParticipantId contributor) {}
  @Override public void onBlipSumbitted(ObservableConversationBlip blip) {}
  @Override public void onBlipTimestampChanged(ObservableConversationBlip blip, long oldTimestamp,
      long newTimestamp) {}
  @Override public void onInlineThreadAdded(ObservableConversationThread thread, int location) {}
  @Override public void onParticipantAdded(ParticipantId participant) {}
  @Override public void onParticipantRemoved(ParticipantId participant) {}
}