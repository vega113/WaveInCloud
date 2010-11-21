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
package org.waveprotocol.wave.client.wavepanel.impl.edit;

import org.waveprotocol.wave.client.wavepanel.impl.WavePanelImpl;
import org.waveprotocol.wave.client.wavepanel.impl.focus.FocusFramePresenter;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipQueueRenderer;

/**
 * Builds and installs wave editing capabilities.
 *
 */
public final class EditActionsBuilder {
  private EditActionsBuilder() {
  }

  /**
   * Builds and installs the wave-editing feature.
   *
   * @param panel wave panel to hold the feature
   * @return the feature.
   */
  public static Actions createAndInstall(WavePanelImpl panel, ModelAsViewProvider views,
      EditSession edit, BlipQueueRenderer blipQueue, FocusFramePresenter focus) {
    focus.addListener(edit);
    if (panel.hasContents()) {
      edit.onInit();
    }
    panel.addListener(focus);
    edit.warmUp();

    Actions actions = new Actions(views, blipQueue, focus, edit);
    EditController.create(actions, panel.getKeyRouter());
    ParticipantController.install(panel, views);
    return actions;
  }
}
