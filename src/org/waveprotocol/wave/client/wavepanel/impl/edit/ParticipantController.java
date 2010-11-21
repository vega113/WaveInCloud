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

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Window;

import org.waveprotocol.wave.client.wavepanel.event.WaveClickHandler;
import org.waveprotocol.wave.client.wavepanel.impl.WavePanelImpl;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantsView;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;
import org.waveprotocol.wave.client.wavepanel.view.dom.DomAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.TypeCodes;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Installs the add/remove participant controls.
 *
 */
public final class ParticipantController implements WaveClickHandler {
  private final DomAsViewProvider views;
  private final ModelAsViewProvider models;

  private ParticipantController(DomAsViewProvider views, ModelAsViewProvider models) {
    this.views = views;
    this.models = models;
  }

  /**
   * Builds and installs the participant control feature.
   *
   * @param panel wave panel to hold the feature
   */
  public static void install(WavePanelImpl panel, ModelAsViewProvider models) {
    ParticipantController controller = new ParticipantController(panel.getViewProvider(), models);
    panel.getHandlers().registerClickHandler(TypeCodes.kind(Type.ADD_PARTICIPANT), controller);
  }

  @Override
  public boolean onClick(ClickEvent event, Element context) {
    ParticipantId p;
    String address = Window.prompt("Add a participant: ", "joe@example.com");
    if (address == null) {
      return true;
    }
    try {
      p = ParticipantId.of(address);
    } catch (InvalidParticipantAddress e) {
      Window.alert("Invalid address");
      return true;
    }

    ParticipantsView participantsUi = views.fromAddButton(context);
    Conversation conversation = models.getParticipants(participantsUi);
    conversation.addParticipant(p);
    return true;
  }
}
