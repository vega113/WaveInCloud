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

package org.waveprotocol.wave.model.supplement;

import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationListenerImpl;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.supplement.SupplementedWaveImpl.DefaultFollow;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.Wavelet;
import org.waveprotocol.wave.model.wave.WaveletListener;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;
import org.waveprotocol.wave.model.wave.opbased.WaveletListenerImpl;

import java.util.Set;

/**
 * Extension of {@link SupplementedWaveImpl} that is also observable.
 *
 */
public final class LiveSupplementedWaveImpl implements ObservableSupplementedWave {

  /** Supplement implementation. */
  private final SupplementedWave target;

  /** Wave view, used to broadcast events in semantic types. */
  private final ObservableWaveView wave;

  /** Conversation model view */
  private final ObservableConversationView conversationView;

  /** Listeners for supplement events. */
  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  private final ObservablePrimitiveSupplement.Listener primitiveListener =
      new ObservablePrimitiveSupplement.Listener() {
        @Override
        public void onArchiveVersionChanged(WaveletId wid, int oldVersion, int newVersion) {
          triggerOnMaybeInboxStateChanged();
        }

        @Override
        public void onArchiveClearChanged(boolean oldValue, boolean newValue) {
          triggerOnMaybeInboxStateChanged();
        }

        @Override
        public void onFolderAdded(int newFolder) {
          triggerOnFolderAdded(newFolder);
        }

        @Override
        public void onFolderRemoved(int oldFolder) {
          triggerOnFolderRemoved(oldFolder);
        }

        @Override
        public void onLastReadBlipVersionChanged(WaveletId wid, String bid, int oldVersion,
            int newVersion) {
          triggerOnMaybeBlipReadChanged(wid, bid);
        }

        @Override
        public void onLastReadParticipantsVersionChanged(WaveletId wid, int oldVersion,
            int newVersion) {
          Wavelet wavelet = wave.getWavelet(wid);
          if (wavelet != null) {
            triggerOnMaybeParticipantsReadChanged(wavelet);
          }
        }

        @Override
        public void onLastReadTagsVersionChanged(WaveletId wid, int oldVersion, int newVersion) {
          Wavelet wavelet = wave.getWavelet(wid);
          if (wavelet != null) {
            triggerOnMaybeTagsReadChanged(wavelet);
          }
        }

        @Override
        public void onLastReadWaveletVersionChanged(WaveletId wid, int oldVersion,
            int newVersion) {
          // Fire maybe change for everything.
          Wavelet wavelet = wave.getWavelet(wid);
          if (wavelet != null) {
            triggerOnMaybeWaveletReadChanged();
          }
        }

        @Override
        public void onFollowed() {
          triggerOnMaybeFollowStateChanged();
        }

        @Override
        public void onUnfollowed() {
          triggerOnMaybeFollowStateChanged();
        }

        @Override
        public void onFollowCleared() {
          triggerOnMaybeFollowStateChanged();
        }

        @Override
        public void onWantedEvaluationsChanged(WaveletId wid) {
          triggerOnWantedEvaluationsChanged(wid);
        }

        @Override
        public void onThreadStateChanged(WaveletId waveletId, String threadId,
            ThreadState oldState, ThreadState newState) {
          ObservableConversation conversation =
              conversationView.getConversation(waveletId.serialise());
          if (conversation != null) {
            ObservableConversationThread thread = conversation.getThread(threadId);
            if (thread != null) {
              triggerOnThreadStateChanged(thread);
            }
          }
        }

        @Override
        public void onGadgetStateChanged(String gadgetId, String key, String oldValue,
            String newValue) {
          triggerOnMaybeGadgetStateChanged(gadgetId);
        }
      };

  private final WaveletListener waveletListener = new WaveletListenerImpl() {
    @Override
    public void onBlipVersionModified(
        ObservableWavelet wavelet, Blip blip, Long oldVersion, Long newVersion) {
      // TODO(hearnden/anorth): Move this to conversation listener when LMVs live there.
      triggerOnMaybeBlipReadChanged(wavelet.getId(), blip.getId());
    }

    @Override
    public void onVersionChanged(ObservableWavelet wavelet, long oldVersion, long newVersion) {
      triggerOnMaybeInboxStateChanged();
      triggerOnMaybeTagsReadChanged(wavelet);
      triggerOnMaybeParticipantsReadChanged(wavelet);
    }
  };

  private final ObservableConversation.Listener conversationListener =
      new ConversationListenerImpl() {
        @Override
        public void onBlipContentDeleted(ObservableConversationBlip blip) {
          triggerOnMaybeBlipReadChanged(blip);
        }

        @Override
        public void onBlipContentUndeleted(ObservableConversationBlip blip) {
          triggerOnMaybeBlipReadChanged(blip);
        }
      };

  public LiveSupplementedWaveImpl(ObservablePrimitiveSupplement supplement,
      ObservableWaveView wave, ParticipantId viewer, DefaultFollow followPolicy,
      ObservableConversationView view) {
    this.target = SupplementedWaveImpl.create(supplement, view, viewer, followPolicy);
    this.wave = wave;
    this.conversationView = view;
    supplement.addListener(primitiveListener);

    ObservableConversationView.Listener viewListener = new ObservableConversationView.Listener() {
      @Override
      public void onConversationAdded(ObservableConversation conversation) {
        conversation.addListener(conversationListener);
        // TODO(user): Once bug 2820511 is fixed, stop listening to the wavelet.
        LiveSupplementedWaveImpl.this.wave.getWavelet(WaveletId.deserialise(conversation.getId()))
            .addListener(waveletListener);
      }

      @Override
      public void onConversationRemoved(ObservableConversation conversation) {
        conversation.removeListener(conversationListener);
        // TODO(user): Once bug 2820511 is fixed, stop listening to the wavelet.
        ((WaveletBasedConversation) conversation).getWavelet().removeListener(waveletListener);
      }
    };

    conversationView.addListener(viewListener);
    for (ObservableConversation conversation : conversationView.getConversations()) {
      viewListener.onConversationAdded(conversation);
    }
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void triggerOnMaybeWaveletReadChanged() {
    for (Listener listener : listeners) {
      listener.onMaybeWaveletReadChanged();
    }
  }

  private void triggerOnMaybeBlipReadChanged(ObservableConversationBlip blip) {
    // TODO(user): P0: make these definite events.
    for (Listener listener : listeners) {
      listener.onMaybeBlipReadChanged(blip);
    }
  }

  private void triggerOnMaybeParticipantsReadChanged(Wavelet wavelet) {
    for (Listener listener : listeners) {
      listener.onMaybeParticipantsReadChanged(wavelet);
    }
  }

  private void triggerOnMaybeTagsReadChanged(Wavelet wavelet) {
    for (Listener listener : listeners) {
      listener.onMaybeTagsReadChanged(wavelet);
    }
  }

  private void triggerOnMaybeInboxStateChanged() {
    for (Listener listener : listeners) {
      listener.onMaybeInboxStateChanged();
    }
  }

  private void triggerOnMaybeFollowStateChanged() {
    for (Listener listener : listeners) {
      listener.onMaybeFollowStateChanged();
    }
  }

  private void triggerOnFolderAdded(int newFolder) {
    for (Listener listener : listeners) {
      listener.onFolderAdded(newFolder);
    }
  }

  private void triggerOnFolderRemoved(int oldFolder) {
    for (Listener listener : listeners) {
      listener.onFolderRemoved(oldFolder);
    }
  }

  private void triggerOnWantedEvaluationsChanged(WaveletId waveletId) {
    for (Listener listener : listeners) {
      listener.onWantedEvaluationsChanged(waveletId);
    }
  }

  private void triggerOnThreadStateChanged(ObservableConversationThread thread) {
    for (Listener listener : listeners) {
      listener.onThreadStateChanged(thread);
    }
  }

  private void triggerOnMaybeGadgetStateChanged(String gadgetId) {
    for (Listener listener : listeners) {
      listener.onMaybeGadgetStateChanged(gadgetId);
    }
  }

  //
  // Converters.
  //

  private ObservableConversationBlip getBlip(WaveletId wid, String bid) {
    ObservableConversation c = conversationView.getConversation(wid.serialise());
    return c != null ? c.getBlip(bid) : null;
  }

  private void triggerOnMaybeBlipReadChanged(WaveletId wid, String bid) {
    ObservableConversationBlip blip = getBlip(wid, bid);
    if (blip != null) {
      triggerOnMaybeBlipReadChanged(blip);
    }
  }

  //
  // Forward SupplementedWave methods to target.
  //

  @Override
  public Set<Integer> getFolders() {
    return target.getFolders();
  }

  @Override
  public void archive() {
    target.archive();
  }

  @Override
  public void see() {
    target.see();
  }

  @Override
  public void see(Wavelet wavelet) {
    target.see(wavelet);
  }

  @Override
  public void inbox() {
    target.inbox();
  }

  @Override
  public boolean isInbox() {
    return target.isInbox();
  }

  @Override
  public boolean isParticipantsUnread(Wavelet wavelet) {
    return target.isParticipantsUnread(wavelet);
  }

  @Override
  public boolean isTagsUnread(Wavelet wavelet) {
    return target.isTagsUnread(wavelet);
  }

  @Override
  public ThreadState getThreadState(ConversationThread thread) {
    return target.getThreadState(thread);
  }

  @Override
  public void setThreadState(ConversationThread thread, ThreadState state) {
    target.setThreadState(thread, state);
  }

  @Override
  @Deprecated
  public boolean isUnread(Blip blip) {
    return target.isUnread(blip);
  }

  @Override
  public boolean isUnread(ConversationBlip blip) {
    return isUnread(blip.hackGetRaw());
  }

  @Override
  public boolean haveParticipantsEverBeenRead(Wavelet wavelet) {
    return target.haveParticipantsEverBeenRead(wavelet);
  }

  @Override
  public void markAsRead() {
    target.markAsRead();
  }

  @Override
  public void markAsRead(ConversationBlip blip) {
    target.markAsRead(blip);
  }

  @Override
  @Deprecated
  public void markAsRead(Blip blip) {
    target.markAsRead(blip);
  }

  @Override
  public void markAsUnread() {
    target.markAsUnread();
  }

  @Override
  public void markParticipantAsRead(Wavelet wavelet) {
    target.markParticipantAsRead(wavelet);
  }

  @Override
  public void markTagsAsRead(Wavelet wavelet) {
    target.markTagsAsRead(wavelet);
  }

  @Override
  public void moveToFolder(int folderId) {
    target.moveToFolder(folderId);
  }

  @Override
  public void mute() {
    target.mute();
  }

  @Override
  public void follow() {
    target.follow();
  }

  @Override
  public void unfollow() {
    target.unfollow();
  }

  @Override
  public boolean isArchived() {
    return target.isArchived();
  }

  @Override
  public boolean isFollowed() {
    // TODO(user): implement properly.
    return !isMute();
  }

  @Override
  public boolean isTrashed() {
    return target.isTrashed();
  }

  @Override
  public boolean isMute() {
    return target.isMute();
  }

  @Override
  public WantedEvaluationSet getWantedEvaluationSet(Wavelet wavelet) {
    return target.getWantedEvaluationSet(wavelet);
  }

  @Override
  public void addWantedEvaluation(WantedEvaluation evaluation) {
    target.addWantedEvaluation(evaluation);
  }

  @Override
  public HashedVersion getSeenVersion(WaveletId id) {
    return target.getSeenVersion(id);
  }

  @Override
  public boolean hasBeenSeen() {
    return target.hasBeenSeen();
  }

  @Override
  public boolean hasPendingNotification() {
    return target.hasPendingNotification();
  }

  @Override
  public void markAsNotified() {
    target.markAsNotified();
  }

  @Override
  public ReadableStringMap<String> getGadgetState(String gadgetId) {
    return target.getGadgetState(gadgetId);
  }

  @Override
  public String getGadgetStateValue(String gadgetId, String key) {
    return target.getGadgetStateValue(gadgetId, key);
  }

  @Override
  public void setGadgetState(String gadgetId, String key, String value) {
    target.setGadgetState(gadgetId, key, value);
  }
}
