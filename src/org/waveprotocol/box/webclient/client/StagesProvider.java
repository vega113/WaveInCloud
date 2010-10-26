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
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.wave.ContentDocumentSinkFactory;
import org.waveprotocol.wave.client.wavepanel.impl.WavePanelImpl;
import org.waveprotocol.wave.client.wavepanel.impl.edit.Actions;
import org.waveprotocol.wave.client.wavepanel.impl.edit.EditActionsBuilder;
import org.waveprotocol.wave.client.wavepanel.impl.edit.EditSession;
import org.waveprotocol.wave.client.wavepanel.impl.focus.FocusFramePresenter;
import org.waveprotocol.wave.client.wavepanel.impl.menu.MenuController;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.EditToolbar;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipQueueRenderer;
import org.waveprotocol.wave.client.widget.common.LogicalPanel;
import org.waveprotocol.wave.client.widget.popup.PopupChromeFactory;
import org.waveprotocol.wave.client.widget.popup.PopupFactory;
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
  protected AsyncHolder<StageThree> createStageThreeLoader(StageTwo two) {
    return new StageThree.DefaultProvider(two) {
      @Override
      protected void install() {
        // This is a verbatim copy of the super method, with extra code below.
        // It is not done as super.install() purely because that requires a
        // change to the libraries repository (to retain a reference to the edit
        // controller), and patches to the libraries repository take too long.

        // Start copy.
        EditorStaticDeps.setPopupProvider(PopupFactory.getProvider());
        EditorStaticDeps.setPopupChromeProvider(PopupChromeFactory.getProvider());

        // Eagerly install some features.
        StageOne stageOne = stageTwo.getStageOne();
        WavePanelImpl panel = stageOne.getWavePanel();
        FocusFramePresenter focus = stageOne.getFocusFrame();
        ModelAsViewProvider views = stageTwo.getModelAsViewProvider();
        ContentDocumentSinkFactory documents = stageTwo.getDocumentRegistry();
        BlipQueueRenderer blipQueue = stageTwo.getBlipQueue();

        EditSession edit = new EditSession(views, documents, panel.getGwtPanel());
        Actions actions = EditActionsBuilder.createAndInstall(panel, views, edit, blipQueue, focus);
        MenuController.install(actions, panel);
        EditToolbar.install(panel, edit);
        // End copy.

        // Do the new-wave flow.
        if (isNewWave) {
          blipQueue.flush();
          edit.startEditing(views.getBlipView(
              stageTwo.getConversations().getRoot().getRootThread().getFirstBlip()));
        }
      }
    };
  }
}
