/**
 * Copyright 2010 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.waveprotocol.wave.examples.fedone.robots;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.event.Event;
import com.google.wave.api.event.OperationErrorEvent;
import com.google.wave.api.event.WaveletBlipCreatedEvent;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.robots.util.ConversationUtil;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.id.WaveletName;

import java.util.Map;

/**
 * Unit tests for {@link OperationContextImpl}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class OperationContextImplTest extends TestCase {

  private static final String WAVE_ID = "example.com!waveid";
  private static final String WAVELET_ID = "example.com!conv+root";
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);
  private static final String ERROR_MESSAGE = "ERROR_MESSAGE";
  private static final String USERNAME = "test@example.com";
  private static final String OPERATION_ID = "op1";

  private EventDataConverter converter;
  private WaveletProvider waveletProvider;
  private OperationRequest request;
  private OperationContextImpl operationContext;
  private RobotWaveletData wavelet;
  private OperationContextImpl boundOperationContext;
  private ConversationUtil conversationUtil;

  @Override
  protected void setUp() throws Exception {
    converter = mock(EventDataConverter.class);
    waveletProvider = mock(WaveletProvider.class);
    conversationUtil = mock(ConversationUtil.class);
    request = new OperationRequest("wave.setTitle", OPERATION_ID);
    operationContext = new OperationContextImpl(waveletProvider, converter, conversationUtil);

    wavelet = mock(RobotWaveletData.class);
    when(wavelet.getWaveletName()).thenReturn(WAVELET_NAME);

    boundOperationContext =
        new OperationContextImpl(waveletProvider, converter, conversationUtil, wavelet);
  }

  public void testConstructResponse() {
    Map<ParamsProperty, Object> data = Maps.newHashMap();
    data.put(ParamsProperty.PARTICIPANT_ID, USERNAME);

    operationContext.constructResponse(request, data);
    JsonRpcResponse response = operationContext.getResponse(request.getId());
    assertFalse("Expected non-error response", response.isError());
    assertEquals("Expected operation id not to change", OPERATION_ID, response.getId());
    assertEquals("Expected payload not to change", data, response.getData());
  }

  public void testConstructErrorResponse() {
    operationContext.constructErrorResponse(request, ERROR_MESSAGE);
    JsonRpcResponse response = operationContext.getResponse(request.getId());
    assertTrue("Expected error response", response.isError());
    assertEquals("Expected provided error message", ERROR_MESSAGE, response.getErrorMessage());
    assertEquals("Expected operation id not to change", OPERATION_ID, response.getId());
  }

  public void testProcessEvent() throws Exception {
    // A randomly selected non-error event
    Event event = new WaveletBlipCreatedEvent(null, null, USERNAME, 0L, "root", "newBlip");

    operationContext.processEvent(request, event);

    JsonRpcResponse response = operationContext.getResponse(request.getId());
    assertFalse("Expected non-error response", response.isError());
    assertEquals("Expected operation id not to change", OPERATION_ID, response.getId());
  }

  public void testProcessErrorEvent() throws Exception {
    // A randomly selected non-error event
    Event event = new OperationErrorEvent(null, null, USERNAME, 0L, OPERATION_ID, ERROR_MESSAGE);
    operationContext.processEvent(request, event);

    JsonRpcResponse response = operationContext.getResponse(request.getId());
    assertTrue("Expected error response", response.isError());
    assertEquals("Expected provided error message", ERROR_MESSAGE, response.getErrorMessage());
    assertEquals("Expected operation id not to change", OPERATION_ID, response.getId());
  }

  public void testContextIsBound() throws Exception {
    assertTrue(boundOperationContext.isBound());
    Map<WaveletName, RobotWaveletData> openWavelets = boundOperationContext.getOpenWavelets();
    assertEquals("Bound wavelet should be open", openWavelets.get(WAVELET_NAME), wavelet);

    // TODO(ljvderijk): Add tests that opening of wavelet outside context can
    // succeed and fail depending on authorization.
  }

  public void testPutNonTemporaryWavelet() throws Exception {
    operationContext.putWavelet(WAVE_ID, WAVELET_ID, wavelet);
    assertEquals(wavelet, operationContext.openWavelet(WAVE_ID, WAVELET_ID));
  }

  public void testPutTemporaryWavelet() throws Exception {
    String tempWaveId = "example.com!" + OperationContextImpl.TEMP_ID_MARKER + "random";
    String tempWaveletId = "example.com!conv+root";
    operationContext.putWavelet(tempWaveId, tempWaveletId, wavelet);
    assertEquals(wavelet, operationContext.openWavelet(tempWaveId, tempWaveletId));
    assertEquals(wavelet, operationContext.openWavelet(WAVE_ID, WAVELET_ID));
  }

  public void testOpenNonExistingWaveletThrowsInvalidRequestException() throws Exception {
    try {
      operationContext.openWavelet(WAVE_ID, WAVELET_ID);
      fail("Expected InvalidRequestException");
    } catch (InvalidRequestException e) {
      // expected
    }
  }

  public void testPutNonTemporaryBlip() throws Exception {
    // Non temporary blip is ignored
    Conversation conversation = mock(Conversation.class);
    ConversationBlip blip = mock(ConversationBlip.class);
    String blipId = "b+1234";
    when(blip.getId()).thenReturn(blipId);
    when(conversation.getBlip(blipId)).thenReturn(blip);

    operationContext.putBlip(blip.getId(), blip);
    assertEquals(operationContext.getBlip(conversation, blipId), blip);
  }

  public void testPutTemporaryBlip() throws Exception {
    Conversation conversation = mock(Conversation.class);
    ConversationBlip blip = mock(ConversationBlip.class);
    String tempBlipId = OperationContextImpl.TEMP_ID_MARKER + "random";
    String blipId = "b+1234";
    when(blip.getId()).thenReturn(blipId);
    when(conversation.getBlip(blipId)).thenReturn(blip);

    operationContext.putBlip(tempBlipId, blip);
    assertEquals("Expected blip for the given tempId",
        operationContext.getBlip(conversation, tempBlipId), blip);
    assertEquals("Expected blip when its non temporary id is given",
        operationContext.getBlip(conversation, blipId), blip);
  }
}
