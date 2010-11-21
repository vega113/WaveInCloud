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
 *
 */
package org.waveprotocol.wave.client.wavepanel.impl.reader;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.client.wavepanel.impl.focus.FocusFramePresenter;
import org.waveprotocol.wave.client.wavepanel.impl.focus.ViewTraverser;
import org.waveprotocol.wave.client.wavepanel.impl.focus.FocusFramePresenter.FocusOrder;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.supplement.SupplementedWave;

/**
 * Inteprets focus-frame movement as reading actions, and also provides an
 * ordering for focus frame movement, based on unread content.
 *
 */
public final class Reader implements FocusFramePresenter.Listener, FocusOrder {
  private final SupplementedWave supplement;
  private final ModelAsViewProvider models;
  private final ViewTraverser traverser;

  @VisibleForTesting
  Reader(SupplementedWave supplement, ModelAsViewProvider models, ViewTraverser traverser) {
    this.supplement = supplement;
    this.models = models;
    this.traverser = traverser;
  }

  /**
   * Builds and installs the reading feature.
   *
   * @return the feature.
   */
  public static Reader createAndInstall(SupplementedWave supplement, FocusFramePresenter focus,
      ModelAsViewProvider models) {
    ViewTraverser traverser = new ViewTraverser();
    Reader reader = new Reader(supplement, models, traverser);
    focus.setOrder(reader);
    focus.addListener(reader);
    return reader;
  }

  @Override
  public void onFocusMoved(BlipView oldUi, BlipView newUi) {
    if (oldUi != null) {
      ConversationBlip oldBlip = models.getBlip(oldUi);
      if (oldBlip != null) {
        stopReading(oldBlip);
      }
    }
    if (newUi != null) {
      ConversationBlip newBlip = models.getBlip(newUi);
      if (newBlip != null) {
        startReading(newBlip);
      }
    }
  }

  void startReading(ConversationBlip blip) {
    supplement.markAsRead(blip);
  }

  void stopReading(ConversationBlip blip) {
    supplement.markAsRead(blip);
  }

  private boolean isRead(BlipView blipUi) {
    return !supplement.isUnread(models.getBlip(blipUi));
  }

  @Override
  public BlipView getNext(BlipView start) {
    BlipView blipUi = traverser.getNext(start);
    while (blipUi != null && isRead(blipUi)) {
      blipUi = traverser.getNext(blipUi);
    }
    return blipUi;
  }

  @Override
  public BlipView getPrevious(BlipView start) {
    BlipView blipUi = traverser.getPrevious(start);
    while (blipUi != null && isRead(blipUi)) {
      blipUi = traverser.getPrevious(blipUi);
    }
    return blipUi;
  }
}
