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
package org.waveprotocol.wave.client;

import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.util.AsyncHolder;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.util.ClientFlags;
import org.waveprotocol.wave.client.wave.ContentDocumentSinkFactory;
import org.waveprotocol.wave.client.wavepanel.impl.WavePanelImpl;
import org.waveprotocol.wave.client.wavepanel.impl.edit.Actions;
import org.waveprotocol.wave.client.wavepanel.impl.edit.EditActionsBuilder;
import org.waveprotocol.wave.client.wavepanel.impl.edit.EditSession;
import org.waveprotocol.wave.client.wavepanel.impl.focus.FocusFramePresenter;
import org.waveprotocol.wave.client.wavepanel.impl.indicator.ReplyIndicatorController;
import org.waveprotocol.wave.client.wavepanel.impl.menu.MenuController;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.EditToolbar;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipQueueRenderer;
import org.waveprotocol.wave.client.widget.popup.PopupChromeFactory;
import org.waveprotocol.wave.client.widget.popup.PopupFactory;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * The third stage of client code.
 * <p>
 * This stage includes editing capabilities.
 *
 */
public interface StageThree{

  Actions getEditActions();

  /**
   * Default implementation of the stage three configuration. Each component is
   * defined by a factory method, any of which may be overridden in order to
   * stub out some dependencies. Circular dependencies are not detected.
   *
   */
  public class DefaultProvider extends AsyncHolder.Impl<StageThree> implements StageThree {
    // External dependencies
    protected final StageTwo stageTwo;

    //
    // Synchronously constructed dependencies.
    //

    private Actions actions;

    public DefaultProvider(StageTwo stageTwo) {
      this.stageTwo = stageTwo;
    }

    /**
     * Creates the second stage.
     */
    @Override
    protected void create(final Accessor<StageThree> whenReady) {
      onStageInit();
      if (ClientFlags.get().enableUndercurrentEditing()) {
        install();
      }
      onStageLoaded();
      whenReady.use(this);
    }

    /** Notifies this provider that the stage is about to be loaded. */
    protected void onStageInit() {
    }

    /** Notifies this provider that the stage has been loaded. */
    protected void onStageLoaded() {
    }

    @Override
    public final Actions getEditActions() {
      return actions == null ? actions = createEditActions() : actions;
    }

    protected Actions createEditActions() {
      StageOne stageOne = stageTwo.getStageOne();
      WavePanelImpl panel = stageOne.getWavePanel();
      FocusFramePresenter focus = stageOne.getFocusFrame();
      ModelAsViewProvider views = stageTwo.getModelAsViewProvider();
      BlipQueueRenderer blipQueue = stageTwo.getBlipQueue();
      ContentDocumentSinkFactory documents = stageTwo.getDocumentRegistry();
      ParticipantId user = stageTwo.getSignedInUser();
      ProfileManager profiles = stageTwo.getProfileManager();

      EditSession edit = new EditSession(views, documents, panel.getGwtPanel());
      Actions actions =
          EditActionsBuilder.createAndInstall(panel, views, profiles, edit, blipQueue, focus);
      MenuController.install(actions, panel);
      EditToolbar.install(panel, edit, user);
      ReplyIndicatorController.install(actions, edit, panel);
      return actions;
    }

    protected void install() {
      EditorStaticDeps.setPopupProvider(PopupFactory.getProvider());
      EditorStaticDeps.setPopupChromeProvider(PopupChromeFactory.getProvider());

      // Eagerly install some features.
      getEditActions();
    }
  }
}
