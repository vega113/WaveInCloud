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

package org.waveprotocol.wave.examples.fedone.agents.echoey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import static org.waveprotocol.wave.examples.fedone.common.DocumentConstants.MANIFEST_DOCUMENT_ID;
import static org.waveprotocol.wave.examples.fedone.util.testing.Matchers.Aliases.contains;
import static org.waveprotocol.wave.examples.fedone.util.testing.Matchers.Aliases.matches;

import com.google.common.collect.Lists;

import org.mockito.ArgumentCaptor;

import org.waveprotocol.wave.examples.fedone.agents.agent.AgentConnection;
import org.waveprotocol.wave.examples.fedone.agents.agent.AgentTestBase;
import org.waveprotocol.wave.examples.fedone.util.BlockingSuccessFailCallback;
import org.waveprotocol.wave.examples.fedone.util.SuccessFailCallback;
import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientUtils;
import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientWaveView;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolSubmitResponse;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreRemoveParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

import java.util.List;

/**
 * Tests for the {@link Echoey} agent.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
public class EchoeyTest extends AgentTestBase<Echoey> {
  // Tests

  /** Should ignore changes to the manifest document. */
  public void testIgnoresChangesToManifest() throws OperationException {
    // We're assuming that Echoey is connected, as tested in AgentTestBase.
    // Otherwise it may ignore changes for the wrong reason.

    CoreWaveletData wavelet = util.createWavelet();
    BufferedDocOp manifest = wavelet.getDocuments().get(MANIFEST_DOCUMENT_ID);
    CoreWaveletDocumentOperation docOp = ClientUtils.appendToManifest(manifest, BLIP_ID);

    // Forget "connect" interactions from setUp.
    reset(backend);
    agent.onDocumentChanged(wavelet, docOp);

    verifyZeroInteractions(backend);
  }

  /** Should ignore changes done by echoey. */
  public void testIgnoresOwnChanges() throws OperationException {
    // We're assuming that Echoey is connected, as tested in AgentTestBase.
    // Otherwise it may ignore changes for the wrong reason.

    String echoeyDocId = BLIP_ID + agent.getEchoeyDocumentSuffix();
    CoreWaveletDocumentOperation docOp = new CoreWaveletDocumentOperation(echoeyDocId,
        ClientUtils.createTextInsertion(MESSAGE, 0, 0));

    // Forget "connect" interactions from setUp.
    reset(backend);
    agent.onDocumentChanged(util.createWavelet(), docOp);

    // Should be at most 1 call to getUserId to identify own changes, and no other calls on the
    // backend.
    verify(backend, atMost(1)).getUserId();
    verifyNoMoreInteractions(backend);
  }

  /** Should exactly mirror incoming changes in a separate blip. */
  public void testMirrorsChangesInSeparateBlip() throws OperationException {
    // Create the wavelet in the backend, otherwise Echoey can't send the delta.
    CoreWaveletData wavelet = util.createWaveletInBackend();
    CoreWaveletDocumentOperation docOp = new CoreWaveletDocumentOperation(BLIP_ID,
        ClientUtils.createTextInsertion(MESSAGE, 0, 0));
    String echoeyDocId = BLIP_ID + agent.getEchoeyDocumentSuffix();

    // Forget interactions from creating the wavelet.
    reset(backend);
    agent.onDocumentChanged(wavelet, docOp);

    List<CoreWaveletDocumentOperation> ops = verifySendDelta(wavelet.getWaveletName());

    // The sent opration should have the same changes as we injected, but in echoey's own blip.
    CoreWaveletDocumentOperation sentOp = ops.get(0);
    assertEquals(echoeyDocId, sentOp.getDocumentId());
    assertEquals(docOp.getOperation(), sentOp.getOperation());
  }

  /** Should create a notification blip when a participant is added. */
  public void testNotifiesParticipantAdded() {
    // Create the wavelet in the backend, otherwise Echoey can't send the delta.
    CoreWaveletData wavelet = util.createWaveletInBackend();
    String addedMessage = agent.getParticipantAddedMessage(OTHER_PARTICIPANT);

    // Forget interactions from creating the wavelet.
    reset(backend);
    agent.onParticipantAdded(wavelet, OTHER_PARTICIPANT);

    List<CoreWaveletDocumentOperation> ops = verifySendDelta(wavelet.getWaveletName());
    String resultingContent = util.getText(ops);

    assertEquals(addedMessage, resultingContent);
  }

  /** Should create a notification blip when a participant is removed. */
  public void testNotifiesParticipantRemoved() {
    // Create the wavelet in the backend, otherwise Echoey can't send the delta.
    CoreWaveletData wavelet = util.createWaveletInBackend();
    String removedMessage = agent.getParticipantRemovedMessage(OTHER_PARTICIPANT);

    // Forget interactions from creating the wavelet.
    reset(backend);
    agent.onParticipantRemoved(wavelet, OTHER_PARTICIPANT);

    List<CoreWaveletDocumentOperation> ops = verifySendDelta(wavelet.getWaveletName());
    String resultingContent = util.getText(ops);

    assertEquals(removedMessage, resultingContent);
  }

  /** Should say hello when added to a wavelet. */
  public void testGreetsWhenAdded() {
    // Create the wavelet in the backend, otherwise Echoey can't send the delta.
    CoreWaveletData wavelet = util.createWaveletInBackend();

    // Forget interactions from creating the wavelet.
    reset(backend);
    agent.onSelfAdded(wavelet);

    List<CoreWaveletDocumentOperation> ops = verifySendDelta(wavelet.getWaveletName());
    String resultingContent = util.getText(ops);

    assertEquals(agent.GREETING, resultingContent);
  }

  /** Should say goodbye when removed from a wavelet. */
  public void testSaysByeWhenRemoved() {
    // Create the wavelet in the backend, otherwise Echoey can't send the delta.
    CoreWaveletData wavelet = util.createWaveletInBackend();

    // Forget interactions from creating the wavelet.
    reset(backend);
    agent.onSelfRemoved(wavelet);

    List<CoreWaveletDocumentOperation> ops = verifySendDelta(wavelet.getWaveletName());
    String resultingContent = util.getText(ops);

    assertEquals(agent.FAREWELL, resultingContent);
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
    CoreWaveletData convRoot = ClientUtils.getConversationRoot(wave);
    WaveletName waveletName = convRoot.getWaveletName();

    // Add another participant.
    callback = BlockingSuccessFailCallback.create();
    backend.sendWaveletOperation(waveletName, new CoreAddParticipant(OTHER_PARTICIPANT),
        callback);
    util.assertOperationComplete(callback);

    // Append a blip by the other participant.
    BufferedDocOp manifest = convRoot.getDocuments().get(MANIFEST_DOCUMENT_ID);
    callback = BlockingSuccessFailCallback.create();
    backend.sendWaveletDelta(waveletName, ClientUtils.createAppendBlipDelta(manifest,
        OTHER_PARTICIPANT, BLIP_ID, MESSAGE), callback);
    util.assertOperationComplete(callback);

    // Remove the other participant.
    callback = BlockingSuccessFailCallback.create();
    backend.sendWaveletOperation(waveletName, new CoreRemoveParticipant(OTHER_PARTICIPANT),
        callback);
    util.assertOperationComplete(callback);

    // Check for the standard Echoey responses to make sure we really did the complete round-trips.
    // We don't get the blips text in-order, so we don't check the order of messages.
    String waveContent = util.getText(wave);
    assertThat(waveContent, contains(agent.GREETING));
    assertThat(waveContent, contains(agent.getParticipantAddedMessage(OTHER_PARTICIPANT)));
    assertThat(waveContent, contains(agent.getParticipantRemovedMessage(OTHER_PARTICIPANT)));
    // There should be one message copy from the other participant and another copy from Echoey.
    assertThat(waveContent, matches(".*" + MESSAGE + ".*" + MESSAGE + ".*"));
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
   * @param waveletName of the wavelet on which we expect a delta.
   * @return the list of operations in the delta.
   */
  @SuppressWarnings("unchecked")
  private List<CoreWaveletDocumentOperation> verifySendDelta(WaveletName waveletName) {
    ArgumentCaptor<CoreWaveletDelta> delta = ArgumentCaptor.forClass(CoreWaveletDelta.class);

    verify(backend).sendWaveletDelta(eq(waveletName), delta.capture(),
        any(SuccessFailCallback.class)); // This results in an "unchecked operation" warning. Is there a better way?

    List<CoreWaveletDocumentOperation> ops = Lists.newArrayList();
    for (CoreWaveletOperation op : delta.getValue().getOperations()) {
      assertThat(op, is(instanceOf(CoreWaveletDocumentOperation.class)));
      ops.add((CoreWaveletDocumentOperation) op);
    }

    return ops;
  }
}
