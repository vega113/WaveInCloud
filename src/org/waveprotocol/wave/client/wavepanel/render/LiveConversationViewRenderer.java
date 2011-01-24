/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.waveprotocol.wave.client.wavepanel.render;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.state.ThreadReadStateMonitor;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ConversationView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantsView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipQueueRenderer.PagingHandler;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.Conversation.Anchor;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.IdentityMap.ProcV;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Iterator;

/**
 * Renderer the conversation update.
 *
 */
public class LiveConversationViewRenderer implements ObservableConversationView.Listener,
    PagingHandler {

  private class ConversationUpdater implements ObservableConversation.Listener,
      ObservableConversation.AnchorListener, PagingHandler {
    private final ObservableConversation conv;
    private final LiveProfileRenderer profileUpdateMonitor;
    private final ThreadReadStateMonitor readMonitor;

    /**
     * @param conv
     */
    public ConversationUpdater(ObservableConversation conv, ProfileManager profileManager,
        ThreadReadStateMonitor readMonitor) {
      this.conv = conv;
      this.profileUpdateMonitor =
          new LiveProfileRenderer(conv, profileManager, viewProvider, shallowBlipRenderer);
      this.readMonitor = readMonitor;
    }

    public void init() {
      conv.addListener((ObservableConversation.AnchorListener) this);
      conv.addListener((ObservableConversation.Listener) this);
      profileUpdateMonitor.setUpParticipantsUpdate(convView);
    }

    public void reset() {
      conv.removeListener((ObservableConversation.AnchorListener) this);
      conv.removeListener((ObservableConversation.Listener) this);
      profileUpdateMonitor.reset();
    }

    @Override
    public void onThreadAdded(ObservableConversationThread thread) {
      ObservableConversationBlip parentBlip = thread.getParentBlip();
      BlipView blipView = viewProvider.getBlipView(parentBlip);

      if (blipView != null) {
        ConversationThread next = findSuccessor(thread, parentBlip.getAllReplyThreads());
        replyHandler.presentBefore(blipView, next, thread);
      } else {
        throw new IllegalStateException("blipView not present");
      }
    }

    @Override
    public void onThreadDeleted(ObservableConversationThread thread) {
      InlineThreadView threadView = viewProvider.getInlineThreadView(thread);
      if (threadView != null) {
        threadView.remove();
      }
    }

    @Override
    public void onBlipAdded(ObservableConversationBlip blip) {
      ConversationThread parentThread = blip.getThread();
      ThreadView threadView = viewOf(parentThread);
      if (threadView != null) {
        // XXX(user): this is incorrect for compound additions, which are
        // processed first to last in document order, so needs to be changed to
        // predecessor-based insertion.
        ConversationBlip ref = findSuccessor(blip, parentThread.getBlips());
        BlipView refView = viewOf(ref);

        // Render the new blip.
        threadView.insertBlipBefore(refView, blip);
        bubbleBlipCountUpdate(blip);
      } else {
        throw new IllegalStateException("threadView not present");
      }
    }

    @Override
    public void onBlipDeleted(ObservableConversationBlip blip) {
      BlipView blipView = viewProvider.getBlipView(blip);
      if (blipView != null) {
        // TODO(user): Hide parent thread if it becomes empty.
        blipView.remove();
      }
      stopListeningToBlip(blip);
      bubbleBlipCountUpdate(blip);
    }

    private void bubbleBlipCountUpdate(ConversationBlip blip) {
      ConversationThread thread = blip.getThread();
      ThreadView threadUi = viewOf(thread);
      threadUi.setTotalBlipCount(readMonitor.getTotalCount(thread));
      ConversationBlip parentBlip = thread.getParentBlip();
      if (parentBlip != null) {
        bubbleBlipCountUpdate(parentBlip);
      }
    }

    /**
     * @param <T>
     * @param cur The object to look for.
     * @param itr The list of iterable to find the object.
     *
     * @return the object that is after the current object in the list of
     *         iterable.
     */
    private <T> T findSuccessor(T cur, Iterable<? extends T> itr) {
      for (Iterator<? extends T> i = itr.iterator(); i.hasNext(); i.next()) {
        if (i.next() == cur) {
          return i.hasNext() ? i.next() : null;
        }
      }
      return null;
    }

    @Override
    public void onBlipContributorAdded(ObservableConversationBlip blip, ParticipantId c) {
      profileUpdateMonitor.addContributorToMonitor(blip, c);
    }

    @Override
    public void onBlipContributorRemoved(ObservableConversationBlip blip, ParticipantId c) {
      profileUpdateMonitor.removeContributorToMonitor(blip, c);
    }


    @Override
    public void onBlipSumbitted(ObservableConversationBlip blip) {
    }

    @Override
    public void onBlipTimestampChanged(ObservableConversationBlip blip, long oldTimestamp,
        long newTimestamp) {
      BlipView blipUi = viewProvider.getBlipView(blip);
      BlipMetaView metaUi = blipUi != null ? blipUi.getMeta() : null;
      if (metaUi != null) {
        shallowBlipRenderer.renderTime(blip, metaUi);
      }
    }

    @Override
    public void onInlineThreadAdded(ObservableConversationThread thread, int location) {
      // inline threads are ignored for now.
    }

    @Override
    public void onParticipantAdded(ParticipantId participant) {
      ParticipantsView participantUi = viewProvider.getParticipantsView(conv);
      participantUi.appendParticipant(conv, participant);
      profileUpdateMonitor.addParticipantToMonitor(participant);
    }

    @Override
    public void onParticipantRemoved(ParticipantId participant) {
      ParticipantView participantUi = viewProvider.getParticipantView(conv, participant);
      if (participantUi != null) {
        participantUi.remove();
      }
      profileUpdateMonitor.removeParticipantToMonitor(participant);
    }

    @Override
    public void pageIn(ConversationBlip blip) {
      // listen to the contributors on the blip
      for (ParticipantId contributor : blip.getContributorIds()) {
        profileUpdateMonitor.addContributorToMonitor(blip, contributor);
      }
    }

    @Override
    public void pageOut(ConversationBlip blip) {
      stopListeningToBlip(blip);
    }

    private void stopListeningToBlip(ConversationBlip blip) {
      // stop listen to the contributors on the blip
      for (ParticipantId contributor : blip.getContributorIds()) {
        profileUpdateMonitor.removeContributorToMonitor(blip, contributor);
      }
    }

    @Override
    public void onAnchorChanged(Anchor oldAnchor, Anchor newAnchor) {
      // Since anchors are application-level immutable, this is a rare case, so
      // the gain in simplicity of implementing it as removal then addition
      // outweighs the efficiency gain from implementing a
      // conversation-view-move mechanism.
      if (oldAnchor != null) {
        // Remove old view.
        ConversationView oldUi = viewOf(conv);
        if (oldUi != null) {
          oldUi.remove();
        }
      }
      if (newAnchor != null) {
        // Insert new view.
        BlipView containerUi = viewOf(newAnchor.getBlip());
        if (containerUi != null) {
          ConversationView convUi = containerUi.insertConversationBefore(null, conv);
        }
      }
    }
  }

  private final ShallowBlipRenderer shallowBlipRenderer;
  private final ModelAsViewProvider viewProvider;
  private final ObservableConversationView convView;
  private final ReplyManager replyHandler;
  private final UpdaterFactory updaterFactory;
  private final LiveSupplementRenderer supplementRenderer;

  private final IdentityMap<Conversation, ConversationUpdater> conversationUpdaters =
      CollectionUtils.createIdentityMap();

  interface UpdaterFactory {
    ConversationUpdater create(ObservableConversation c);
  }

  /**
   */
  public LiveConversationViewRenderer(ShallowBlipRenderer shallowBlipRenderer,
      ModelAsViewProvider viewProvider,
      ReplyManager replyHandler,
      ObservableConversationView convView,
      ObservableSupplementedWave supplement,
      final ProfileManager profileManager,
      final ThreadReadStateMonitor readMonitor) {
    this.shallowBlipRenderer = shallowBlipRenderer;
    this.viewProvider = viewProvider;
    this.replyHandler = replyHandler;
    this.convView = convView;

    // TODO(hearnden/reuben): DI this with a public static factory method, not a
    // public constructor. Or, make the profile monitor cross-conversation, so
    // that participantManager does not need to be passed around.
    this.updaterFactory = new UpdaterFactory() {
      @Override
      public ConversationUpdater create(ObservableConversation c) {
        return new ConversationUpdater(c, profileManager, readMonitor);
      }
    };
    this.supplementRenderer = LiveSupplementRenderer.create(supplement, viewProvider, readMonitor);

    // Attach to existing conversations, then keep it live.
    for (ObservableConversation conv : convView.getConversations()) {
      observe(conv);
    }
    convView.addListener(this);
  }

  /**
   * Destroys this renderer, releasing its resources. It is no longer usable
   * after a call to this method.
   */
  public void reset() {
    conversationUpdaters.each(new ProcV<Conversation, ConversationUpdater>() {
      @Override
      public void apply(Conversation key, ConversationUpdater value) {
        value.reset();
      }
    });
    supplementRenderer.destroy();
  }

  /**
   * Observes a conversation, updating its view as it changes.
   *
   * @param c conversation to observe
   */
  private void observe(ObservableConversation c) {
    ConversationUpdater updater = updaterFactory.create(c);
    updater.init();
    conversationUpdaters.put(c, updater);
  }

  /**
   * Stops observing a conversation, releasing any resources that were used to
   * observe it.
   *
   * @param c conversation to stop observing
   */
  private void unobserve(ObservableConversation c) {
    ConversationUpdater updater = conversationUpdaters.get(c);
    if (updater != null) {
      conversationUpdaters.remove(c);
      updater.reset();
    }
  }

  private ThreadView viewOf(ConversationThread thread) {
    return thread == null ? null // \u2620
        : (thread.getConversation().getRootThread() == thread) // \u2620
            ? viewProvider.getRootThreadView(thread) // \u2620
            : viewProvider.getInlineThreadView(thread);
  }

  private BlipView viewOf(ConversationBlip ref) {
    return ref == null ? null : viewProvider.getBlipView(ref);
  }

  private ConversationView viewOf(Conversation ref) {
    return ref == null ? null : viewProvider.getConversationView(ref);
  }

  @Override
  public void pageIn(ConversationBlip blip) {
    ConversationUpdater waveletListener = conversationUpdaters.get(blip.getConversation());
    Preconditions.checkState(waveletListener != null);
    waveletListener.pageIn(blip);
  }

  @Override
  public void pageOut(ConversationBlip blip) {
    ConversationUpdater waveletListener = conversationUpdaters.get(blip.getConversation());
    Preconditions.checkState(waveletListener != null);
    waveletListener.pageOut(blip);
  }

  //
  // Note: the live maintenance of nested conversations is not completely
  // correct, because the conversation model does not broadcast correct and
  // consistent events. The rendering is only as correct as the model events,
  // and it is not considered to be worthwhile for the rendering to generate the
  // correct events manually rather than wait for the model events to be fixed.
  //
  // Additionally, the conversation model does not expose the conversations
  // anchored at a particular blip, which makes a stable sibling ordering of
  // conversations infeasible.
  //

  @Override
  public void onConversationAdded(ObservableConversation conversation) {
    BlipView container = viewOf(conversation.getAnchor().getBlip());
    if (container != null) {
      ConversationView conversationUi = container.insertConversationBefore(null, conversation);
    }

    observe(conversation);
  }

  @Override
  public void onConversationRemoved(ObservableConversation conversation) {
    unobserve(conversation);

    ConversationView convUi = viewOf(conversation);
    if (convUi != null) {
      convUi.remove();
    }
  }
}
