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

import static org.mockito.Mockito.mock;

import com.google.wave.api.BlipData;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationType;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.data.converter.EventDataConverter;

import junit.framework.TestCase;

import org.waveprotocol.box.server.common.HashedVersionFactoryImpl;
import org.waveprotocol.box.server.common.VersionedWaveletDelta;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.OperationContextImpl;
import org.waveprotocol.box.server.robots.RobotWaveletData;
import org.waveprotocol.box.server.robots.operations.AppendBlipService;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.BasicWaveletOperationContextFactory;
import org.waveprotocol.wave.model.operation.wave.ConversionUtil;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.version.DistinctVersion;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipationHelper;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.impl.EmptyWaveletSnapshot;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.List;

/**
 * Unit tests for {@link AppendBlipService}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class AppendBlipServiceTest extends TestCase {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionFactoryImpl(URI_CODEC);
  private static final DocumentFactory<DocumentOperationSink> DOCUMENT_FACTORY =
      BasicFactories.muteDocumentFactory();
  private static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");
  private static final String WAVE_ID = "example.com!waveid";
  private static final String WAVELET_ID = "example.com!conv+root";
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);
  private static final BasicWaveletOperationContextFactory CONTEXT_FACTORY =
      new BasicWaveletOperationContextFactory(ALEX);

  private AppendBlipService service;
  private WaveletProvider waveletProvider;
  private EventDataConverter converter;
  private ObservableWaveletData waveletData;
  private OpBasedWavelet wavelet;
  private ObservableConversation conversation;
  private HashedVersion hashedVersionZero;
  private OperationContextImpl context;

  @Override
  protected void setUp() {
    service = AppendBlipService.create();
    waveletProvider = mock(WaveletProvider.class);
    converter = mock(EventDataConverter.class);

    waveletData = WaveletDataImpl.Factory.create(DOCUMENT_FACTORY).create(new EmptyWaveletSnapshot(
        WaveId.deserialise(WAVE_ID), WaveletId.deserialise(WAVELET_ID), ALEX, 0L));
    waveletData.addParticipant(ALEX);

    SilentOperationSink<WaveletOperation> executor =
        SilentOperationSink.Executor.build(waveletData);
    wavelet =
        new OpBasedWavelet(waveletData.getWaveId(), waveletData, CONTEXT_FACTORY,
            ParticipationHelper.IGNORANT, executor, SilentOperationSink.VOID);

    // Make a conversation
    WaveletBasedConversation.makeWaveletConversational(wavelet);

    ConversationUtil conversationUtil = new ConversationUtil(FakeIdGenerator.create());
    conversation = conversationUtil.getConversation(wavelet).getRoot();
    conversation.getRootThread().appendBlip();

    hashedVersionZero = HASH_FACTORY.createVersionZero(WAVELET_NAME);

    context = new OperationContextImpl(waveletProvider, converter, conversationUtil);
    context.putWavelet(WAVE_ID, WAVELET_ID, new RobotWaveletData(waveletData, hashedVersionZero));
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

    service.execute(operation, context, ALEX);
    // Call helper method to apply all the operations in the context
    applyOperations();

    JsonRpcResponse response = context.getResponse(operationId);
    assertFalse(response.isError());

    // Retrieve the blip using the context so that the temp blip storage is
    // checked
    ConversationBlip newBlip = context.getBlip(conversation, tempBlipId);
    assertEquals("The response didn't contain the id of the new blip", newBlip.getId(),
        response.getData().get(ParamsProperty.NEW_BLIP_ID));
    String actualContent = newBlip.getContent().toXmlString();
    assertTrue("Expected the new blip to contain the contens as specified in the operation",
        actualContent.contains(content));
  }

  /**
   * Applies the operations present in the result for a single wavelet.
   */
  private void applyOperations() throws OperationException {
    RobotWaveletData robotWavelet = context.getOpenWavelets().get(WAVELET_NAME);
    List<VersionedWaveletDelta> deltas = robotWavelet.getDeltas();

    for (VersionedWaveletDelta vWDelta : deltas) {
      CoreWaveletDelta delta = vWDelta.delta;
      DistinctVersion endVersion = DistinctVersion.of(
          delta.getTargetVersion().getVersion() + delta.getOperations().size(), -1);
      List<WaveletOperation> ops = ConversionUtil.fromCoreWaveletDelta(delta, 0L, endVersion);
      for (WaveletOperation op : ops) {
        op.apply(waveletData);
      }
    }
  }
}
