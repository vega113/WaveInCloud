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

package org.waveprotocol.box.server.robots.operations;

import com.google.wave.api.BlipData;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.event.WaveletBlipCreatedEvent;

import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

/**
 * {@link OperationService} for the "wavelet.appendBlip" method.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class AppendBlipService implements OperationService {

  private AppendBlipService() {
  }

  @Override
  public void execute(
      OperationRequest operation, OperationContext context, ParticipantId participant)
      throws InvalidRequestException {
    BlipData blipData = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_DATA);

    OpBasedWavelet wavelet = context.getWavelet(operation, participant);
    ObservableConversationView conversationView = context.getConversation(wavelet);

    String waveletId = OperationUtil.getRequiredParameter(operation, ParamsProperty.WAVELET_ID);
    ObservableConversation conversation = conversationView.getConversation(waveletId);
    // Add the new blip and store it in the context
    ObservableConversationBlip newBlip = conversation.getRootThread().appendBlip();
    context.putBlip(blipData.getBlipId(), newBlip);

    String content = blipData.getContent();

    if (content.length() > 0 && content.charAt(0) == '\n') {
      // While the client libraries force a newline to be sent as the first
      // character we'll remove it here since the new blip we created already
      // contains a newline.
      content = content.substring(1);
    }

    XmlStringBuilder builder = XmlStringBuilder.createText(content);
    LineContainers.appendToLastLine(newBlip.getContent(), builder);

    WaveletBlipCreatedEvent event =
        new WaveletBlipCreatedEvent(null, null, participant.getAddress(),
            System.currentTimeMillis(), conversation.getRootThread().getFirstBlip().getId(),
            newBlip.getId());
    context.processEvent(operation, event);
  }

  public static AppendBlipService create() {
    return new AppendBlipService();
  }
}
