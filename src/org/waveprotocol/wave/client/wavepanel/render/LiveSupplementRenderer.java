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
package org.waveprotocol.wave.client.wavepanel.render;

import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;

/**
 * Listens to supplement updates and update the read state of blips.
 *
 */
public final class LiveSupplementRenderer extends ObservableSupplementedWave.ListenerImpl {
  private final ModelAsViewProvider views;
  private final ObservableSupplementedWave supplement;

  LiveSupplementRenderer(ObservableSupplementedWave supplement, ModelAsViewProvider views) {
    this.supplement = supplement;
    this.views = views;
  }

  public static LiveSupplementRenderer create(ObservableSupplementedWave supplement,
      ModelAsViewProvider views) {
    LiveSupplementRenderer renderer = new LiveSupplementRenderer(supplement, views);
    supplement.addListener(renderer);
    return renderer;
  }

  public void destroy() {
    supplement.removeListener(this);
  }

  @Override
  public void onMaybeBlipReadChanged(ObservableConversationBlip blip) {
    BlipView blipUi = views.getBlipView(blip);
    BlipMetaView metaUi = blipUi != null ? blipUi.getMeta() : null;

    if (metaUi != null) {
      metaUi.setRead(!supplement.isUnread(blip));
    }
  }
}
