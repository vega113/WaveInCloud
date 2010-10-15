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
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.OperationType;

import junit.framework.TestCase;

import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.OperationContextImpl;
import org.waveprotocol.box.server.robots.testing.OperationServiceHelper;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Unit tests for {@link AppendBlipService}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class AppendBlipServiceTest extends TestCase {

  private static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");
  private static final String WAVE_ID = "example.com!waveid";
  private static final String WAVELET_ID = "example.com!conv+root";
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);

  private AppendBlipService service;
  private OperationServiceHelper helper;

  @Override
  protected void setUp() {
    service = AppendBlipService.create();
    helper = new OperationServiceHelper(WAVELET_NAME, ALEX);
  }

  public void testAppedBlipService() throws Exception {
    String operationId = "op1";
    String tempBlipId = OperationContext.TEMP_ID_MARKER + "blip1";

    String content = "Hello World";
    BlipData blipData = new BlipData(WAVE_ID, WAVELET_ID, tempBlipId, content);
    // BlipData constructor is broken, it doesn't set the blipId passed in the
    // constructor
    blipData.setBlipId(tempBlipId);
    OperationRequest operation =
        new OperationRequest(OperationType.WAVELET_APPEND_BLIP.method(), operationId, WAVE_ID,
            WAVELET_ID, Parameter.of(ParamsProperty.BLIP_DATA, blipData));

    OperationContextImpl context = helper.getContext();
    service.execute(operation, context, ALEX);

    JsonRpcResponse response = context.getResponse(operationId);
    assertFalse(response.isError());

    ObservableConversation conversation =
        context.getConversation(context.openWavelet(WAVE_ID, WAVELET_ID, ALEX)).getRoot();
    // Retrieve the blip using the context so that the temp blip storage is
    // checked
    ConversationBlip newBlip = context.getBlip(conversation, tempBlipId);
    assertEquals("The response didn't contain the id of the new blip", newBlip.getId(),
        response.getData().get(ParamsProperty.NEW_BLIP_ID));
    String actualContent = newBlip.getContent().toXmlString();
    assertTrue("Expected the new blip to contain the contens as specified in the operation",
        actualContent.contains(content));
  }
}
