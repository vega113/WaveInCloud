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

package org.waveprotocol.box.agents.echoey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.waveprotocol.box.common.DocumentConstants.MANIFEST_DOCUMENT_ID;
import static org.waveprotocol.box.server.util.testing.Matchers.Aliases.contains;

import com.google.common.collect.Lists;

import org.mockito.ArgumentCaptor;
import org.waveprotocol.box.client.ClientUtils;
import org.waveprotocol.box.client.ClientWaveView;
import org.waveprotocol.box.server.agents.agent.AgentConnection;
import org.waveprotocol.box.server.agents.agent.AgentTestBase;
import org.waveprotocol.box.server.util.BlockingSuccessFailCallback;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.server.waveserver.WaveClientRpc.ProtocolSubmitResponse;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests for the {@link Echoey} agent.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
public class EchoeyTest extends AgentTestBase<Echoey> {
  private static final WaveletOperationContext OP_CONTEXT = new WaveletOperationContext(
      PARTICIPANT, 1234567890L, 1);

  // Tests

  /** Should ignore changes to the manifest document. */
  public void testIgnoresChangesToManifest() throws OperationException {
    // We're assuming that Echoey is connected, as tested in AgentTestBase.
    // Otherwise it may ignore changes for the wrong reason.

    WaveletData wavelet = util.createWavelet();
    BlipData manifest = wavelet.getDocument(MANIFEST_DOCUMENT_ID);
    WaveletBlipOperation docOp = ClientUtils.appendToManifest(manifest, BLIP_ID, OP_CONTEXT);

    // Forget "connect" interactions from setUp.
    reset(backend);
    agent.onDocumentChanged(wavelet, docOp.getBlipId(), docOp.getBlipOp());

    verifyZeroInteractions(backend);
  }

  /** Should ignore changes done by echoey. */
  public void testIgnoresOwnChanges() throws OperationException {
    // We're assuming that Echoey is connected, as tested in AgentTestBase.
    // Otherwise it may ignore changes for the wrong reason.

    String echoeyDocId = BLIP_ID + agent.getEchoeyDocumentSuffix();
    WaveletBlipOperation docOp = new WaveletBlipOperation(echoeyDocId,
        new BlipContentOperation(OP_CONTEXT,
        ClientUtils.createTextInsertion(MESSAGE, 0, 0)));

    // Forget "connect" interactions from setUp.
    reset(backend);
    agent.onDocumentChanged(util.createWavelet(), docOp.getBlipId(), docOp.getBlipOp());

    // Should be at most 1 call to getUserId to identify own changes, and no other calls on the
    // backend.
    verify(backend, atMost(1)).getUserId();
    verifyNoMoreInteractions(backend);
  }

  /** Should exactly mirror incoming changes in a separate blip. */
  public void testMirrorsChangesInSeparateBlip() {
    // Create the wavelet in the backend, otherwise Echoey can't send the delta.
    WaveletData wavelet = util.createWaveletInBackend();
    WaveletBlipOperation docOp = new WaveletBlipOperation(BLIP_ID,
        new BlipContentOperation(OP_CONTEXT,
        ClientUtils.createTextInsertion(MESSAGE, 0, 0)));
    String echoeyDocId = BLIP_ID + agent.getEchoeyDocumentSuffix();

    // Forget interactions from creating the wavelet.
    reset(backend);
    agent.onDocumentChanged(wavelet, docOp.getBlipId(), docOp.getBlipOp());

    List<WaveletBlipOperation> ops = verifySendDelta(wavelet);

    // The sent opration should have the same changes as we injected, but in
    // echoey's own blip.
    WaveletBlipOperation sentOp = ops.get(0);
    assertEquals(echoeyDocId, sentOp.getBlipId());
    assertEquals(docOp.getBlipOp(), sentOp.getBlipOp());
  }

  /** Should create a notification blip when a participant is added. */
  public void testNotifiesParticipantAdded() {
    // Create the wavelet in the backend, otherwise Echoey can't send the delta.
    WaveletData wavelet = util.createWaveletInBackend();
    String addedMessage = agent.getParticipantAddedMessage(OTHER_PARTICIPANT);

    // Forget interactions from creating the wavelet.
    reset(backend);
    agent.onParticipantAdded(wavelet, OTHER_PARTICIPANT);

    List<WaveletBlipOperation> ops = verifySendDelta(wavelet);
    String resultingContent = util.getText(ops);

    assertEquals(addedMessage, resultingContent);
  }

  /** Should create a notification blip when a participant is removed. */
  public void testNotifiesParticipantRemoved() {
    // Create the wavelet in the backend, otherwise Echoey can't send the delta.
    WaveletData wavelet = util.createWaveletInBackend();
    String removedMessage = agent.getParticipantRemovedMessage(OTHER_PARTICIPANT);

    // Forget interactions from creating the wavelet.
    reset(backend);
    agent.onParticipantRemoved(wavelet, OTHER_PARTICIPANT);

    List<WaveletBlipOperation> ops = verifySendDelta(wavelet);
    String resultingContent = util.getText(ops);

    assertEquals(removedMessage, resultingContent);
  }

  /** Should say hello when added to a wavelet. */
  public void testGreetsWhenAdded() {
    // Create the wavelet in the backend, otherwise Echoey can't send the delta.
    WaveletData wavelet = util.createWaveletInBackend();

    // Forget interactions from creating the wavelet.
    reset(backend);
    agent.onSelfAdded(wavelet);

    List<WaveletBlipOperation> ops = verifySendDelta(wavelet);
    String resultingContent = util.getText(ops);

    assertEquals(Echoey.GREETING, resultingContent);
  }

  /** Should say goodbye when removed from a wavelet. */
  public void testSaysByeWhenRemoved() {
    // Create the wavelet in the backend, otherwise Echoey can't send the delta.
    WaveletData wavelet = util.createWaveletInBackend();

    // Forget interactions from creating the wavelet.
    reset(backend);
    agent.onSelfRemoved(wavelet);

    List<WaveletBlipOperation> ops = verifySendDelta(wavelet);
    String resultingContent = util.getText(ops);

    assertEquals(Echoey.FAREWELL, resultingContent);
  }

  /**
   * Test a complete interaction with round-trips to a fake server. Check that it doesn't timeout
   * or cause any errors, and that it actually completes the full round-trip (receives events,
   * handles them, and gets a response back from the server).
   */
  // TODO(Michael): Add a timeout to this test when we switch to JUnit4.
  public void testCompletesInteraction() {
    BlockingSuccessFailCallback<ProtocolSubmitResponse, String> callback;

    // Create a wave and add Echoey to it.
    callback = BlockingSuccessFailCallback.create();
    ClientWaveView wave = backend.createConversationWave(callback);
    util.assertOperationComplete(callback);

    // Get the wave details.
    WaveletData convRoot = ClientUtils.getConversationRoot(wave);
    WaveletName waveletName = WaveletDataUtil.waveletNameOf(convRoot);

    // Add another participant.
    callback = BlockingSuccessFailCallback.create();
    backend.sendWaveletOperations(waveletName, callback,
        new AddParticipant(OP_CONTEXT, OTHER_PARTICIPANT));
    util.assertOperationComplete(callback);

    // Append a blip by the other participant.
    // This test cheats by using the agent's own backend to send operations,
    // so the added blip is ignored.
    // TODO(anorth): Restore this when either the test or
    // backend is appropriately factored to allow changing author.
//    BlipData manifest = convRoot.getDocument(MANIFEST_DOCUMENT_ID);
//    callback = BlockingSuccessFailCallback.create();
//    backend.sendWaveletOperations(waveletName, callback,
//        ClientUtils.createAppendBlipOps(manifest, BLIP_ID, MESSAGE));
//    util.assertOperationComplete(callback);

    // Remove the other participant.
    callback = BlockingSuccessFailCallback.create();
    backend.sendWaveletOperations(waveletName, callback,
        new RemoveParticipant(OP_CONTEXT, OTHER_PARTICIPANT));
    util.assertOperationComplete(callback);

    // Check for the standard Echoey responses to make sure we really did the complete round-trips.
    // We don't get the blips text in-order, so we don't check the order of messages.
    String waveContent = util.getText(wave);
    assertThat(waveContent, contains(Echoey.GREETING));
    assertThat(waveContent, contains(agent.getParticipantAddedMessage(OTHER_PARTICIPANT)));
    assertThat(waveContent, contains(agent.getParticipantRemovedMessage(OTHER_PARTICIPANT)));

    // There should be one message copy from the other participant and another copy from Echoey.
    // TODO(anorth): restore this after factoring backend to allow author setting.
//    assertThat(waveContent, matches(".*" + MESSAGE + ".*" + MESSAGE + ".*"));
  }

  // Utility methods

  @Override
  protected Echoey createAgent(AgentConnection connection) {
    return new Echoey(connection);
  }

  /**
   * Verifies that sendWaveletDelta has been called and that the delta contains only
   * {@code WaveletDocumentOperation}s.
   *
   * @param wavelet the wavelet on which we expect a delta.
   * @return the list of operations in the delta.
   */
  private List<WaveletBlipOperation> verifySendDelta(WaveletData wavelet) {
    WaveletName waveletName = WaveletDataUtil.waveletNameOf(wavelet);
    ArgumentCaptor<WaveletOperation> opCaptor =
        ArgumentCaptor.forClass(WaveletOperation.class);

    verify(backend, atMost(1)).sendAndAwaitWaveletOperations(eq(waveletName), anyLong(),
        any(TimeUnit.class), opCaptor.capture());
    verify(backend, atMost(1)).sendAndAwaitWaveletOperations(eq(waveletName), anyLong(),
        any(TimeUnit.class), opCaptor.capture(), opCaptor.capture());
    verify(backend, atMost(1)).sendAndAwaitWaveletOperations(eq(waveletName), anyLong(),
        any(TimeUnit.class), opCaptor.capture(), opCaptor.capture(), opCaptor.capture());

    List<WaveletBlipOperation> ops = Lists.newArrayList();
    for (WaveletOperation op : opCaptor.getAllValues()) {
      assertThat(op, is(instanceOf(WaveletBlipOperation.class)));
      ops.add((WaveletBlipOperation) op);
    }
    return ops;
  }
}
