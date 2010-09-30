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

package org.waveprotocol.wave.examples.fedone.robots.passive;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.OperationType;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.data.converter.EventDataConverterManager;
import com.google.wave.api.event.EventType;
import com.google.wave.api.robot.Capability;

import junit.framework.TestCase;

import org.mockito.internal.stubbing.answers.ThrowsException;
import org.waveprotocol.wave.examples.common.HashedVersion;
import org.waveprotocol.wave.examples.common.HashedVersionFactory;
import org.waveprotocol.wave.examples.common.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.examples.fedone.account.RobotAccountData;
import org.waveprotocol.wave.examples.fedone.account.RobotAccountDataImpl;
import org.waveprotocol.wave.examples.fedone.robots.OperationContextImpl;
import org.waveprotocol.wave.examples.fedone.robots.OperationResults;
import org.waveprotocol.wave.examples.fedone.robots.OperationServiceRegistry;
import org.waveprotocol.wave.examples.fedone.robots.RobotCapabilities;
import org.waveprotocol.wave.examples.fedone.robots.RobotWaveletData;
import org.waveprotocol.wave.examples.fedone.robots.operations.DoNothingService;
import org.waveprotocol.wave.examples.fedone.robots.operations.OperationService;
import org.waveprotocol.wave.examples.fedone.robots.util.ConversationUtil;
import org.waveprotocol.wave.examples.fedone.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.examples.fedone.util.WaveletDataUtil;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletProvider;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;

import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link RobotOperationApplicator}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotOperationApplicatorTest extends TestCase {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());
  private static final HashedVersionFactory HASH_FACTORY =
      new HashedVersionZeroFactoryImpl(URI_CODEC);
  private static final String WAVE_ID = "example.com!waveid";
  private static final String WAVELET_ID = "example!conv+root";
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);
  private static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");
  private static final ParticipantId ROBOT_PARTICIPANT =
      ParticipantId.ofUnsafe("robot@example.com");
  private static final RobotAccountData ACCOUNT =
      new RobotAccountDataImpl(ROBOT_PARTICIPANT, "www.example.com", new RobotCapabilities(
          Maps.<EventType, Capability> newHashMap(), "fake", ProtocolVersion.DEFAULT), true);

  private EventDataConverterManager converterManager;
  private WaveletProvider waveletProvider;
  private OperationServiceRegistry operationRegistry;
  private RobotOperationApplicator applicator;
  private OperationContextImpl context;

  @Override
  protected void setUp() {
    converterManager = mock(EventDataConverterManager.class);
    waveletProvider = mock(WaveletProvider.class);
    operationRegistry = mock(OperationServiceRegistry.class);
    ConversationUtil conversationUtil = mock(ConversationUtil.class);

    applicator = new RobotOperationApplicator(
        converterManager, waveletProvider, operationRegistry, conversationUtil);

    EventDataConverter converter = mock(EventDataConverter.class);
    context = new OperationContextImpl(waveletProvider, converter, conversationUtil);
  }

  public void testExecuteOperationsExecutes() throws Exception {
    String operationId = "op1";
    OperationRequest operation = new OperationRequest("wavelet.create", operationId);
    List<OperationRequest> operations = Collections.singletonList(operation);

    OperationService service = DoNothingService.create();
    when(operationRegistry.getServiceFor(any(OperationType.class))).thenReturn(service);

    applicator.executeOperations(context, operations, ACCOUNT);
    assertTrue("Expected one response", context.getResponses().size() == 1);
    assertFalse("Expected a succesful response", context.getResponse(operationId).isError());
  }

  public void testExecuteOperationsSetsErrorInvalidParticipantAddressException() throws Exception {
    String operationId = "op1";
    OperationRequest operation = new OperationRequest(
        "wavelet.create", operationId, Parameter.of(ParamsProperty.PROXYING_FOR, "invalid#$%^"));
    List<OperationRequest> operations = Collections.singletonList(operation);

    OperationService service = DoNothingService.create();
    when(operationRegistry.getServiceFor(any(OperationType.class))).thenReturn(service);

    applicator.executeOperations(context, operations, ACCOUNT);
    assertTrue("Expected one response", context.getResponses().size() == 1);
    assertTrue("Expected an error response", context.getResponse(operationId).isError());
  }

  public void testExecuteOperationsSetsErrorOnInvalidRequestException() throws Exception {
    String operationId = "op1";
    OperationRequest operation = new OperationRequest("wavelet.create", operationId);
    List<OperationRequest> operations = Collections.singletonList(operation);

    OperationService service =
        mock(OperationService.class, new ThrowsException(new InvalidRequestException("")));
    when(operationRegistry.getServiceFor(any(OperationType.class))).thenReturn(service);

    applicator.executeOperations(context, operations, ACCOUNT);
    assertTrue("Expected one response", context.getResponses().size() == 1);
    assertTrue("Expected an error response", context.getResponse(operationId).isError());
  }

  public void testHandleResultsSubmitsDelta() throws Exception {
    ObservableWaveletData waveletData = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, ALEX, 0L);
    DocInitialization content = new DocInitializationBuilder().build();
    waveletData.createBlip("b+example", ROBOT_PARTICIPANT,
        Collections.singletonList(ROBOT_PARTICIPANT), content, 0L, 0);

    HashedVersion hashedVersionZero = HASH_FACTORY.createVersionZero(WAVELET_NAME);
    RobotWaveletData wavelet = new RobotWaveletData(waveletData, hashedVersionZero);

    // Perform an operation that will be put into a delta
    wavelet.getOpBasedWavelet(ROBOT_PARTICIPANT).addParticipant(ALEX);

    OperationResults results = mock(OperationResults.class);
    when(results.getOpenWavelets()).thenReturn(Collections.singletonMap(WAVELET_NAME, wavelet));

    applicator.handleResults(results, ACCOUNT);

    verify(waveletProvider).submitRequest(eq(WAVELET_NAME), any(ProtocolWaveletDelta.class),
        any(WaveletProvider.SubmitRequestListener.class));
  }

  public void testGetOperationAuthor() throws Exception {
    // Type of operation doesn't matter
    OperationRequest operation = new OperationRequest("wavelet.create", "op1");
    ParticipantId author = applicator.getOperationAuthor(operation, ACCOUNT);
    assertEquals(author, ROBOT_PARTICIPANT);

    String proxy = "proxy";
    operation = new OperationRequest(
        "wavelet.create", "op1", Parameter.of(ParamsProperty.PROXYING_FOR, proxy));
    ParticipantId proxyAuthor = applicator.getOperationAuthor(operation, ACCOUNT);

    ParticipantId proxyParticipant = ParticipantId.of("robot+" + proxy + "@example.com");
    assertEquals(proxyAuthor, proxyParticipant);
  }

  public void testGetOperationAuthorThrowsException() {
    // Type of operation doesn't matter
    OperationRequest operation = new OperationRequest(
        "wavelet.create", "op1", Parameter.of(ParamsProperty.PROXYING_FOR, "invalid#$%^"));
    try {
      applicator.getOperationAuthor(operation, ACCOUNT);
      fail("Expected InvalidParticpantAddress since proxy in malformed");
    } catch (InvalidParticipantAddress e) {
      // Expected
    }
  }
}
