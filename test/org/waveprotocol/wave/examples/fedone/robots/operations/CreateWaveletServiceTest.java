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

package org.waveprotocol.wave.examples.fedone.robots.operations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.OperationType;
import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.impl.WaveletData;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.common.VersionedWaveletDelta;
import org.waveprotocol.wave.examples.fedone.robots.OperationContext;
import org.waveprotocol.wave.examples.fedone.robots.OperationContextImpl;
import org.waveprotocol.wave.examples.fedone.robots.RobotWaveletData;
import org.waveprotocol.wave.examples.fedone.robots.util.ConversationUtil;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link CreateWaveletService}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class CreateWaveletServiceTest extends TestCase {

  private static final String OPERATION_ID = "op1";
  private static final String MESSAGE = "message";
  private static final String TEMP_WAVE_ID =
      "example.com!" + OperationContext.TEMP_ID_MARKER + "waveId";
  private static final String WAVELET_ID = "example.com!waveletid";
  private static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");
  private static final ParticipantId BOB = ParticipantId.ofUnsafe("bob@example.com");
  private static final String MALFORMED_ADDRESS = "malformed!@@#$%(*)^_^@@.com";

  private CreateWaveletService service;
  private WaveletProvider waveletProvider;
  private EventDataConverter converter;
  private OperationContextImpl context;
  private WaveletData waveletData;
  private OperationRequest operation;

  @Override
  protected void setUp() {
    service = CreateWaveletService.create();
    waveletProvider = mock(WaveletProvider.class);
    converter = mock(EventDataConverter.class);

    waveletData = mock(WaveletData.class);
    when(waveletData.getWaveId()).thenReturn(TEMP_WAVE_ID);
    when(waveletData.getWaveletId()).thenReturn(WAVELET_ID);
    when(waveletData.getRootBlipId()).thenReturn("b+root");

    ConversationUtil conversationUtil = new ConversationUtil(FakeIdGenerator.create());
    context = new OperationContextImpl(waveletProvider, converter, conversationUtil);

    operation =
        new OperationRequest(OperationType.ROBOT_CREATE_WAVELET.method(), OPERATION_ID,
            Parameter.of(ParamsProperty.WAVELET_DATA, waveletData),
            Parameter.of(ParamsProperty.MESSAGE, MESSAGE));
  }

  public void testCreateWaveletService() throws Exception {
    when(waveletData.getParticipants()).thenReturn(Collections.singletonList(BOB.getAddress()));

    service.execute(operation, context, ALEX);

    JsonRpcResponse response = context.getResponse(OPERATION_ID);
    assertFalse(response.isError());
    Map<ParamsProperty, Object> responseData = response.getData();
    assertEquals("Expected message to be set", MESSAGE, responseData.get(ParamsProperty.MESSAGE));

    String waveId = (String) responseData.get(ParamsProperty.WAVE_ID);
    String waveletId = (String) responseData.get(ParamsProperty.WAVELET_ID);
    RobotWaveletData newWavelet = context.getOpenWavelets().get(WaveletName.of(waveId, waveletId));
    assertNotNull("A new wavelet must be open", newWavelet);

    List<VersionedWaveletDelta> deltas = newWavelet.getDeltas();
    List<CoreWaveletOperation> operations = deltas.get(0).delta.getOperations();
    boolean seenAddAlex = false;
    boolean seenAddBob = false;
    for (CoreWaveletOperation op : operations) {
      if (op instanceof CoreAddParticipant) {
        CoreAddParticipant addParticipant = (CoreAddParticipant) op;
        if (addParticipant.getParticipantId().equals(ALEX)) {
          seenAddAlex = true;
        } else if (addParticipant.getParticipantId().equals(BOB)) {
          seenAddBob = true;
        } else {
          fail("No one else but Alex and Bob should be added");
        }
      }
    }
    assertTrue("Alex was not added", seenAddAlex);
    assertTrue("Bob was not added", seenAddBob);
  }

  public void testCreateWaveletServiceThrowsOnInvalidParticipant() throws Exception {
    when(waveletData.getParticipants()).thenReturn(Collections.singletonList(MALFORMED_ADDRESS));
    try {
      service.execute(operation, context, ALEX);
      fail("Expected InvalidRequestException");
    } catch (InvalidRequestException e) {
      // expected
    }
  }
}
