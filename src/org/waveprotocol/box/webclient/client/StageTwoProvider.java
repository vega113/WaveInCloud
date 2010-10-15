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

import com.google.gwt.user.client.Command;

import org.waveprotocol.wave.client.StageOne;
import org.waveprotocol.wave.client.StageTwo;
import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileListener;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.account.testing.FakeProfile;
import org.waveprotocol.wave.client.common.util.AsyncHolder;
import org.waveprotocol.wave.client.wave.ContentDocumentSinkFactory;
import org.waveprotocol.wave.client.wave.RegistriesHolder;
import org.waveprotocol.wave.client.wavepanel.impl.reader.Reader;
import org.waveprotocol.wave.client.wavepanel.render.FullDomWaveRendererImpl;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.WaveRenderer;
import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeDocument;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl.WaveletConfigurator;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl.WaveletFactory;

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


  /**
   * @param waveId the id of the wave to open. If null, it means, create a new wave.
   * @param channel communication channel
   */
  public StageTwoProvider(StageOne stageOne, WaveId waveId, RemoteViewServiceMultiplexer channel) {
    super(stageOne);
    this.waveId = waveId;
    this.channel = channel;
  }

  @Override
  protected ContentDocumentSinkFactory createDocumentRegistry() {
    return ContentDocumentSinkFactory.create(new ConversationSchemas(), RegistriesHolder.get());
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
  protected ParticipantId createSignedInUser() {
    return ParticipantId.ofUnsafe(Session.get().getAddress());
  }

  @Override
  protected WaveViewService createWaveViewService() {
    return new RemoteWaveViewService(waveId, channel, getDocumentRegistry());
  }

  /**
   * Swaps order of open and render.
   */
  @Override
  protected void install() {
    // Activate liveness.
    createUpgrader().connect(new Command() {
      @Override
      public void execute() {
        WaveRenderer waveRenderer =
          FullDomWaveRendererImpl.create(getConversations(), getProfileManager(), getBlipDetailer(),
              getViewIdMapper(), getBlipQueue());

        stageOne.getDomAsViewProvider().setRenderer(waveRenderer);
        // Ensure the wave is rendered.
        renderWave(waveRenderer);

        // Eagerly install some features.
        Reader.createAndInstall(getSupplement(), stageOne.getFocusFrame(), getModelAsViewProvider());
      }
    });
  }


  @Override
  protected void fetchWave(final AsyncHolder.Accessor<WaveViewData> whenReady) {
    // TODO(zdwang): Create a real wave, For now, when there is no wave to open, use a fake wave.
    if (waveId == null) {
      whenReady.use(WaveFactory.create(getDocumentRegistry()));
    } else {
//      // Re-enable this once the protocol supports connecting on versions.
//      SnapshotFetcher.fetchWave(waveId, new SimpleCallback<WaveViewData, Throwable>() {
//
//        @Override
//        public void onSuccess(WaveViewData response) {
//          whenReady.use(response);
//        }
//
//        @Override
//        public void onFailure(Throwable reason) {
//          throw new RuntimeException("Unable to handle failed fetchWave.", reason);
//        }
//      }, getDocumentRegistry());
      // Use an empty view as the initial state, since the protocol will send
      // snapshots when opened.
      whenReady.use(WaveViewDataImpl.create(waveId));
    }
  }


  /**
   * Creates a sample wave with a conversation in it.
   */
  private final static class WaveFactory {

    /**
     * Creates a sample wave.
     *
     * @param docFactory factory/registry for documents in the wave
     * @return the wave state of the sample wave.
     */
    public static WaveViewDataImpl create(ContentDocumentSinkFactory docFactory) {
      // Create a sample wave.
      WaveViewData sampleData = createSampleWave();

      // Now build one that has the same setup state as that required by
      // undercurrent (complex issue with the per-document output sinks).
      WaveViewDataImpl newData = WaveViewDataImpl.create(sampleData.getWaveId());
      WaveletDataImpl.Factory copier = WaveletDataImpl.Factory.create(docFactory);
      for (ReadableWaveletData src : sampleData.getWavelets()) {
        WaveletDataImpl copied = copier.create(src);
        for (ParticipantId p : src.getParticipants()) {
          copied.addParticipant(p);
        }
        copied.setTitle(src.getTitle());
        copied.setVersion(copied.getVersion());
        copied.setDistinctVersion(src.getDistinctVersion());
        copied.setLastModifiedTime(src.getLastModifiedTime());
        newData.addWavelet(copied);
      }
      return newData;
    }

    /** @return a sample wave with a conversation in it. */
    private static WaveViewData createSampleWave() {
      final ParticipantId sampleAuthor = ParticipantId.ofUnsafe("nobody@example.com");
      IdGenerator gen = FakeIdGenerator.create();
      final WaveViewDataImpl waveData = WaveViewDataImpl.create(gen.newWaveId());
      final FakeDocument.Factory docFactory = BasicFactories.fakeDocumentFactory();
      final ObservableWaveletData.Factory<?> waveletDataFactory =
          new ObservableWaveletData.Factory<WaveletDataImpl>() {
            private final ObservableWaveletData.Factory<WaveletDataImpl> inner =
                WaveletDataImpl.Factory.create(docFactory);

            @Override
            public WaveletDataImpl create(ReadableWaveletData data) {
              WaveletDataImpl wavelet = inner.create(data);
              waveData.addWavelet(wavelet);
              return wavelet;
            }
          };
      WaveletFactory<OpBasedWavelet> waveletFactory = new WaveletFactory<OpBasedWavelet>() {
        final WaveletFactory<OpBasedWavelet> inner =
            BasicFactories.opBasedWaveletFactoryBuilder().with(waveletDataFactory).with(
                sampleAuthor).build();

        @Override
        public OpBasedWavelet create(WaveId waveId, WaveletId waveletId, ParticipantId creator) {
          OpBasedWavelet wavelet = inner.create(waveId, waveletId, creator);
          docFactory.registerSinkFactory(wavelet);
          return wavelet;
        }
      };
      WaveViewImpl<?> wave = WaveViewImpl.create(
          waveletFactory, waveData.getWaveId(), gen, sampleAuthor, WaveletConfigurator.ADD_CREATOR);

      // Build a conversation in that wave.
      ConversationView v = WaveBasedConversationView.create(wave, gen);
      Conversation c = v.createRoot();
      ConversationThread root = c.getRootThread();
      sampleReply(root.appendBlip());
      write(root.appendBlip());
      write(root.appendBlip());
      biggestSampleReply(root.appendBlip());
      write(root.appendBlip());

      return waveData;
    }

    private static void write(ConversationBlip blip) {
      org.waveprotocol.wave.model.document.Document d = blip.getContent();
      d.emptyElement(d.getDocumentElement());
      d.appendXml(XmlStringBuilder.createFromXmlString("<body><line></line>Hello World</body>"));
    }

    private static void sampleReply(ConversationBlip blip) {
      write(blip);
      ConversationThread thread = blip.appendInlineReplyThread(8);
      write(thread.appendBlip());
    }

    private static void biggerSampleReply(ConversationBlip blip) {
      write(blip);
      ConversationThread thread = blip.appendReplyThread();
      sampleReply(thread.appendBlip());
      sampleReply(thread.appendBlip());
      write(thread.appendBlip());
    }

    private static void biggestSampleReply(ConversationBlip blip) {
      write(blip);
      ConversationThread thread = blip.appendInlineReplyThread(8);
      biggerSampleReply(thread.appendBlip());
      thread = blip.appendInlineReplyThread(9);
      biggerSampleReply(thread.appendBlip());
      write(thread.appendBlip());
    }
  }
}
