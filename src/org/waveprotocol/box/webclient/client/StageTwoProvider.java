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


package org.waveprotocol.box.webclient.client;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.Command;

import org.waveprotocol.wave.client.StageOne;
import org.waveprotocol.wave.client.StageTwo;
import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileListener;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.account.testing.FakeProfile;
import org.waveprotocol.wave.client.common.util.AsyncHolder;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.wavepanel.impl.reader.Reader;
import org.waveprotocol.wave.client.wavepanel.render.FullDomWaveRendererImpl;
import org.waveprotocol.wave.client.wavepanel.view.ModelIdMapper;
import org.waveprotocol.wave.client.wavepanel.view.ModelIdMapperImpl;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.WaveRenderer;
import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;

/**
 * Provides stage 2 of the staged loading of the wave panel
 *
 * @author zdwang@google.com (David Wang)
 */
public class StageTwoProvider extends StageTwo.DefaultProvider {

  static class ProfileImpl implements Profile {

    private final ParticipantId id;

    public ProfileImpl(ParticipantId id) {
      this.id = id;
    }

    @Override
    public ParticipantId getParticipantId() {
      return id;
    }

    @Override
    public String getAddress() {
      return id.getAddress();
    }

    @Override
    public String getFullName() {
      return capitalize(id.getAddress());
    }

    @Override
    public String getFirstName() {
      return capitalize(id.getAddress());
    }

    private String capitalize(String s) {
      return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public String getImageUrl() {
      return "static/images/unknown.jpg";
    }
  }

  /**
   * Placeholder for proper profile management.
   */
  static class ProfileManagerImpl implements ProfileManager {
    @Override
    public FakeProfile getProfile(ParticipantId participantId) {
      return new FakeProfile(participantId);
    }

    @Override
    public boolean shouldIgnore(ParticipantId participant) {
      return false;
    }

    @Override
    public void addListener(ParticipantId profile, ProfileListener listener) {
    }

    @Override
    public void removeListener(ParticipantId profile, ProfileListener listener) {
    }

  }

  private final WaveId waveId;
  private final RemoteViewServiceMultiplexer channel;
  private final boolean isNewWave;
  // TODO: Remove this after WebClientBackend is deleted.
  private final IdGenerator idGenerator;

  /**
   * Continuation to progress to the next stage. This will disappear with the
   * new protocol.
   */
  private AsyncHolder.Accessor<StageTwo> whenReady;

  /**
   * @param waveId the id of the wave to open, or null to create a new wave
   * @param channel communication channel
   * @param idGenerator
   */
  public StageTwoProvider(StageOne stageOne, WaveId waveId, RemoteViewServiceMultiplexer channel,
      boolean isNewWave, IdGenerator idGenerator) {
    super(stageOne);
    Preconditions.checkArgument(stageOne != null);
    Preconditions.checkArgument(waveId != null);
    this.waveId = waveId;
    this.channel = channel;
    this.isNewWave = isNewWave;
    this.idGenerator = idGenerator;
  }

  @Override
  protected SchemaProvider createSchemas() {
    return new ConversationSchemas();
  }

  @Override
  protected ProfileManager createProfileManager() {
    return new ProfileManagerImpl();
  }

  @Override
  protected String createSessionId() {
    return Session.get().getIdSeed();
  }

  @Override
  protected IdGenerator createIdGenerator() {
    return idGenerator;
  }

  @Override
  protected ParticipantId createSignedInUser() {
    return ParticipantId.ofUnsafe(Session.get().getAddress());
  }

  @Override
  protected WaveViewService createWaveViewService() {
    return new RemoteWaveViewService(waveId, channel, getDocumentRegistry());
  }

  /** @return the id mangler for model objects. Subclasses may override. */
  private static int n;
  @Override
  protected ModelIdMapper createModelIdMapper() {
    // Note: for some reason, detached elements still retain presence in
    // document.getElementById(). So all per-wave id generation need to have a
    // seed that is unique across the entire client session.
    return ModelIdMapperImpl.create(getConversations(), "U" + n++);
  }


  /**
   * Swaps order of open and render.
   */
  @Override
  protected void install() {
    if (isNewWave) {
      // For a new wave, initial state comes from local initialization.
      Conversation c = getConversations().createRoot();
      c.addParticipant(getSignedInUser());
      c.getRootThread().appendBlip();
      super.install();
      whenReady.use(StageTwoProvider.this);
    } else {

      SchedulerInstance.getLowPriorityTimer().scheduleDelayed(new Task() {
        @Override
        public void execute() {
          // For an existing wave, while we're still using the old protocol,
          // rendering must be delayed until the channel is opened, because the
          // initial state snapshots come from the channel.
          createUpgrader().connect(new Command() {
            @Override
            public void execute() {
              WaveRenderer waveRenderer =
                  FullDomWaveRendererImpl.create(getConversations(), getProfileManager(),
                      getBlipDetailer(), getViewIdMapper(), getBlipQueue());

              stageOne.getDomAsViewProvider().setRenderer(waveRenderer);

              // Ensure the wave is rendered.
              renderWave(waveRenderer);

              // Eagerly install some features.
              Reader.createAndInstall(
                  getSupplement(), stageOne.getFocusFrame(), getModelAsViewProvider());

              // Rendering, and therefore the whole stage is now ready.
              whenReady.use(StageTwoProvider.this);
            }
          });
        }}, 200);
    }
  }

  @Override
  protected void create(final AsyncHolder.Accessor<StageTwo> whenReady) {
    this.whenReady = whenReady;
    super.create(new AsyncHolder.Accessor<StageTwo>() {
      @Override
      public void use(StageTwo x) {
        // Delay progression until rendering is ready.
      }
    });
  }

  @Override
  protected void fetchWave(final AsyncHolder.Accessor<WaveViewData> whenReady) {
    whenReady.use(WaveViewDataImpl.create(waveId));
  }
}
