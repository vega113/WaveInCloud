/**
 * Copyright 2011 Google Inc.
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

package org.waveprotocol.wave.client.wavepanel.impl.title;

import org.waveprotocol.wave.client.doodad.title.TitleAnnotationHandler;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.wavepanel.impl.edit.EditSession;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.LineContainers;

/**
 * Automatically sets the wave title to the first line of the root blip on the edit session end if:
 * <ol>
 * <li>The edited blip is a root blip.</li>
 * <li>Or the wave title is empty.</li>
 * </ol>
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public final class WaveTitleHandler implements EditSession.Listener {

  private final EditSession editSession;
  private final ModelAsViewProvider views;
  
  private static boolean hastTitleAnnotation(CMutableDocument mutable) {
    int docSize = mutable.size();
    return mutable.firstAnnotationChange(0, docSize, TitleAnnotationHandler.KEY, null) != -1;
  }

  /**
   * Sets or replaces an automatic title for the wave based on the first line of the root
   * blip. 
   */
  private static void computeAndSetWaveTitle(CMutableDocument mutable) {
    // Sets or replaces the wave title.
    mutable.setAnnotation(0, 1, TitleAnnotationHandler.KEY,
        DocHelper.getTextForElement(mutable, LineContainers.LINE_TAGNAME));
  }
  
  public static WaveTitleHandler install(EditSession editSession, ModelAsViewProvider views) {
    return new WaveTitleHandler(editSession, views);
  }
  
  private WaveTitleHandler(EditSession editSession, ModelAsViewProvider views) {
    this.views = views;
    this.editSession = editSession;
    init();
  }
  
  private void init() {
    editSession.addListener(this);
  }

  @Override
  public void onSessionStart(Editor e, BlipView blipUi) {
  }

  @Override
  public void onSessionEnd(Editor editor, BlipView blipUi) {
  }

  @Override
  public void onSessionPreEnd(Editor editor, BlipView blipUi) {
    // Set the wave title if needed.
    if (blipUi != null) {
      CMutableDocument mutable = editor.getDocument();
      ConversationBlip editBlip = views.getBlip(blipUi);
      if (editBlip.isRoot() || !hastTitleAnnotation(mutable)) {
        computeAndSetWaveTitle(mutable);
      }
    }
  }
}
