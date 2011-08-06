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

import com.google.gwt.user.client.Window;

import org.waveprotocol.wave.client.doodad.title.TitleAnnotationHandler;
import org.waveprotocol.wave.client.wavepanel.WavePanel;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.LineContainers;

/**
 * Sets the browser window title to the wave title. 
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public final class WindowTitleHandler implements WavePanel.ExtendedLifecycleListener {

  
  private static final String DEFAULT_TITLE = "Communicate and collaborate in real-time";
  
  private final WavePanel panel;
  private final ModelAsViewProvider views;
  
  public static WindowTitleHandler install(WavePanel panel, ModelAsViewProvider views) {
    return new WindowTitleHandler(panel, views);
  }
  
  private WindowTitleHandler(WavePanel panel, ModelAsViewProvider views) {
    this.panel = panel;
    this.views = views;
    init();
  }
  
  private void init() {
    panel.addListener(this);
  }
  
  @Override
  public void onInit() {
  }

  @Override
  public void onReset() {
    Window.setTitle(DEFAULT_TITLE);
  }

  @Override
  public void onLoad(BlipView blipUi, boolean isRootBlip) {
    String waveTitle = "";
    if (blipUi != null && isRootBlip) {
      ConversationBlip blip = views.getBlip(blipUi);
      Document document = blip.getContent();
      int docSize = document.size();
      int location = document.firstAnnotationChange(0, docSize, TitleAnnotationHandler.KEY, null);
      if (location != -1) {
        waveTitle = document.getAnnotation(location, TitleAnnotationHandler.KEY);
      } else {
        // Use the first line of the blip as title.
        waveTitle =
          DocHelper.getTextForElement(document, LineContainers.LINE_TAGNAME);
      }
    }
    if (waveTitle.isEmpty() || waveTitle.length() < 3) {
      waveTitle = DEFAULT_TITLE;
    }
    Window.setTitle(waveTitle);
  }
}
