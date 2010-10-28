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

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.StageOne;
import org.waveprotocol.wave.client.StageThree;
import org.waveprotocol.wave.client.StageTwo;
import org.waveprotocol.wave.client.StageZero;
import org.waveprotocol.wave.client.Stages;
import org.waveprotocol.wave.client.common.util.AsyncHolder;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipQueueRenderer;
import org.waveprotocol.wave.client.widget.common.LogicalPanel;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;

/**
 * Stages for loading the undercurrent Wave Panel
 *
 * @author zdwang@google.com (David Wang)
 */
public class StagesProvider extends Stages {

  private final Element wavePanelElement;
  private final LogicalPanel rootPanel;
  private final WaveId waveId;
  private final RemoteViewServiceMultiplexer channel;
  private final IdGenerator idGenerator;
  private final boolean isNewWave;

  /**
   * @param wavePanelElement The dom element to become the wave panel
   * @param rootPanel A panel that this an ancestor of wavePanelElement. This is
   *        used for adopting to the GWT widget tree.
   * @param waveId the id of the wave to open. If null, it means, create a new
   *        wave.
   * @param channel communication channel.
   * @param isNewWave true if the wave is a new client-created wave
   * @param idGenerator
   */
  public StagesProvider(Element wavePanelElement, LogicalPanel rootPanel, WaveId waveId,
      RemoteViewServiceMultiplexer channel, IdGenerator idGenerator, boolean isNewWave) {
    this.wavePanelElement = wavePanelElement;
    this.rootPanel = rootPanel;
    this.waveId = waveId;
    this.channel = channel;
    this.idGenerator = idGenerator;
    this.isNewWave = isNewWave;
  }

  @Override
  protected AsyncHolder<StageOne> createStageOneLoader(StageZero zero) {
    return new StageOne.DefaultProvider(zero) {
      @Override
      protected Element createWaveHolder() {
        return wavePanelElement;
      }

      @Override
      protected LogicalPanel createWaveContainer() {
        return rootPanel;
      }
    };
  }

  @Override
  protected AsyncHolder<StageTwo> createStageTwoLoader(StageOne one) {
    return new StageTwoProvider(one, waveId, channel, isNewWave, idGenerator);
  }

  @Override
  protected AsyncHolder<StageThree> createStageThreeLoader(final StageTwo two) {
    return new StageThree.DefaultProvider(two) {

      @Override
      protected void create(final Accessor<StageThree> whenReady) {
        // Prepend an init wave flow onto the stage continuation.
        super.create(new Accessor<StageThree>() {
          @Override
          public void use(StageThree x) {
            if (isNewWave) {
              initNewWave(x);
            }
            whenReady.use(x);
          }
        });
      }

      private void initNewWave(StageThree three) {
        // Do the new-wave flow.
        ModelAsViewProvider views = two.getModelAsViewProvider();
        BlipQueueRenderer blipQueue = two.getBlipQueue();
        ConversationView wave = two.getConversations();

        // Force rendering to finish.
        blipQueue.flush();
        BlipView blipUi = views.getBlipView(wave.getRoot().getRootThread().getFirstBlip());
        three.getEditActions().startEditing(blipUi);
      }
    };
  }
}
