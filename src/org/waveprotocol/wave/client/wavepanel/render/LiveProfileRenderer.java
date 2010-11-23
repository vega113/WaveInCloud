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

import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileListener;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.SerialQueueProcessor;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.StringSet;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Listens to profiles update and update the avatars images for a Conversation.
 *
 */
class LiveProfileRenderer implements ProfileListener {
  /**
   * A sentinel value used to indicate that the participants panel needs to be updated as well.
   * This is used to specific the participants panel update in the same data structure.
   * Technically it is a valid blip id, but it is unlikely to appears in real
   * life.
   */
  private final static String PARTICIPANT_FAKE_BLIP_ID = "!@\u2620#!#!@#";
  private final ProfileManager profileManager;

  private final ModelAsViewProvider viewProvider;

  private final Conversation conversation;

  private final ShallowBlipRenderer shallowBlipRenderer;

  /**
   * A map of participant address to a collection of blipId +
   * PARTICIPANT_FAKE_BLIP_ID. They represents of blips to update for a profile
   * update. If a blipId of PARTICIPANT_FAKE_BLIP_ID is in the set, the
   * participants should be updated as well.
   * The keySet of the blipContributorToUpdate is all the profile we are listening to.
   */
  private final StringMap<StringSet> blipContributorToUpdate =
      CollectionUtils.createStringMap();

  /**
   * The set of blips in contributor updater waiting to be executed.
   */
  private final StringSet blipsWaitingToBeUpdated = CollectionUtils.createStringSet();

  private final SerialQueueProcessor<String> contributorUpdater =
      new SerialQueueProcessor<String>(SchedulerInstance.getLowPriorityTimer()) {
        @Override
        public void process(String blipId) {
          blipsWaitingToBeUpdated.remove(blipId);
          ConversationBlip blip = conversation.getBlip(blipId);
          BlipView blipUi = blip != null ? viewProvider.getBlipView(blip) : null;
          BlipMetaView metaUi = blipUi != null ? blipUi.getMeta() : null;
          if (metaUi != null) {
            shallowBlipRenderer.renderContributors(blip, metaUi);
          }
        }
      };

  LiveProfileRenderer(Conversation conv, ProfileManager profileManager,
      ModelAsViewProvider viewProvider, ShallowBlipRenderer shallowBlipRenderer) {
    this.profileManager = profileManager;
    this.conversation = conv;
    this.viewProvider = viewProvider;
    this.shallowBlipRenderer = shallowBlipRenderer;
  }

  public void reset() {
    blipContributorToUpdate.each(
      new ProcV<StringSet>() {
        @Override
        public void apply(String key, StringSet value) {
          profileManager.removeListener(new ParticipantId(key), LiveProfileRenderer.this);
        }
      });
    blipContributorToUpdate.clear();
  }

  public void setUpParticipantsUpdate(ConversationView convView) {
    for (Conversation conv : convView.getConversations()) {
      for (ParticipantId id : conv.getParticipantIds()) {
        addParticipantToMonitor(id);
      }
    }
  }

  public void addParticipantToMonitor(ParticipantId id) {
    addProfileToMonitor(PARTICIPANT_FAKE_BLIP_ID, id);
  }

  public void removeParticipantToMonitor(ParticipantId id) {
    removeProfileToMonitor(PARTICIPANT_FAKE_BLIP_ID, id);
  }

  /**
   * Adds a contributor to monitor for a particular blip.
   *
   * @param blip
   * @param id
   */
  public void addContributorToMonitor(ConversationBlip blip, ParticipantId id) {
    String blipId = blip.getId();
    addProfileToMonitor(blipId, id);
  }

  /**
   * Removes a contributor to monitor for a particular blip.
   *
   * @param blip
   * @param id
   */
  public void removeContributorToMonitor(ConversationBlip blip, ParticipantId id) {
    String blipId = blip.getId();
    removeProfileToMonitor(blipId, id);
  }

  private void addProfileToMonitor(String blipId, ParticipantId id) {
    StringSet contributorSet = blipContributorToUpdate.get(id.getAddress());
    if (contributorSet == null) {
      profileManager.addListener(id, this);
      contributorSet = CollectionUtils.createStringSet();
      blipContributorToUpdate.put(id.getAddress(), contributorSet);
    }

    if (!blipId.equals(PARTICIPANT_FAKE_BLIP_ID)) {
      scheduleUpdateForBlip(blipId);
    }
    contributorSet.add(blipId);
  }

  private void removeProfileToMonitor(String blipId, ParticipantId id) {
    StringSet contributorSet = blipContributorToUpdate.get(id.getAddress());
    if (contributorSet != null) {
      contributorSet.remove(blipId);

      if (contributorSet.isEmpty()) {
        profileManager.removeListener(id, this);
        blipContributorToUpdate.remove(id.getAddress());
      }
    }
  }

  @Override
  public void onProfileUpdated(final Profile profile) {
    String address = profile.getAddress();
    blipContributorToUpdate.get(address).each(new Proc() {
      @Override
      public void apply(String blipId) {
        if (blipId.equals(PARTICIPANT_FAKE_BLIP_ID)) {
          ParticipantView participantUi =
              viewProvider.getParticipantView(conversation, profile.getParticipantId());
          if (participantUi != null) {
            render(profile, participantUi);
          }
        } else {
          scheduleUpdateForBlip(blipId);
        }
      }
    });
  }

  private void scheduleUpdateForBlip(String blipId) {
    if (!blipsWaitingToBeUpdated.contains(blipId)) {
      blipsWaitingToBeUpdated.add(blipId);
      contributorUpdater.add(blipId);
    }
  }

  /**
   * Renders a profile into a participant view.
   */
  private void render(Profile profile, ParticipantView participantUi) {
    participantUi.setAvatar(profile.getImageUrl());
    participantUi.setName(profile.getFullName());
  }
}
