/**
 * Copyright 2009 Google Inc.
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
package org.waveprotocol.wave.examples.fedone.waveserver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import junit.framework.TestCase;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.waveprotocol.wave.examples.fedone.common.CommonConstants.INDEX_WAVE_ID;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import static org.waveprotocol.wave.examples.fedone.common.HashedVersion.unsigned;
import org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer;
import static org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer.serialize;
import org.waveprotocol.wave.examples.fedone.waveserver.ClientFrontend.OpenListener;
import static org.waveprotocol.wave.examples.fedone.waveserver.ClientFrontendImpl.DIGEST_AUTHOR;
import static org.waveprotocol.wave.examples.fedone.waveserver.ClientFrontendImpl.DIGEST_DOCUMENT_ID;
import static org.waveprotocol.wave.examples.fedone.waveserver.ClientFrontendImpl.createUnsignedDeltas;
import static org.waveprotocol.wave.examples.fedone.waveserver.ClientFrontendImpl.indexWaveletNameFor;
import static org.waveprotocol.wave.examples.fedone.waveserver.ClientFrontendImpl.waveletNameForIndexWavelet;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import static org.waveprotocol.wave.model.id.IdConstants.CONVERSATION_ROOT_WAVELET;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.waveserver.SubmitResultListener;

import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 */
public class ClientFrontendImplTest extends TestCase {
  private static final WaveId WAVE_ID = new WaveId("domain", "waveId");
  private static final WaveletId WAVELET_ID = new WaveletId("domain", CONVERSATION_ROOT_WAVELET);
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);
  private static final WaveletName INDEX_WAVELET_NAME =
    WaveletName.of(INDEX_WAVE_ID, new WaveletId("domain", "waveId"));
  private static final ParticipantId USER = new ParticipantId("user@host.com");
  // waveletIdPrefixes to use when subscribing to all wavelets
  private static final Set<String> ALL_WAVELETS = ImmutableSet.of("");
  private static final HashedVersion VERSION_0 = HashedVersion.versionZero(WAVELET_NAME);
  private static final ProtocolWaveletDelta DELTA =
    serialize(new WaveletDelta(USER, ImmutableList.of(new AddParticipant(USER))), VERSION_0);
  private static final DeltaSequence DELTAS =
    new DeltaSequence(ImmutableList.of(DELTA), serialize(HashedVersion.unsigned(1L)));
  private static final Map<String, BufferedDocOp> DOCUMENT_STATE = ImmutableMap.of();

  private ClientFrontendImpl clientFrontend;
  private WaveletProvider waveletProvider;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    waveletProvider = mock(WaveletProvider.class);
    this.clientFrontend = new ClientFrontendImpl(waveletProvider);
    verify(waveletProvider).setListener((WaveletListener) isNotNull());
  }

  /**
   * Test that a wavelet name can be converted to the corresponding index
   * wavelet name if and only if the wavelet's domain equals the wave's domain
   * and the wavelet's ID is CONVERSATION_ROOT_WAVELET.
   */
  public void testIndexWaveletNameConversion() {
    assertEquals(INDEX_WAVELET_NAME, indexWaveletNameFor(WAVELET_NAME));
    assertEquals(WAVELET_NAME, waveletNameForIndexWavelet(INDEX_WAVELET_NAME));

    WaveletName invalid =
      WaveletName.of(WAVE_ID, new WaveletId("otherdomain", CONVERSATION_ROOT_WAVELET));
    try {
      indexWaveletNameFor(invalid);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      //pass
    }
    invalid = WaveletName.of(WAVE_ID, new WaveletId("domain", "otherId"));
    try {
      indexWaveletNameFor(invalid);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      //pass
    }

    invalid = WaveletName.of(INDEX_WAVE_ID,
        new WaveletId(INDEX_WAVE_ID.getDomain(), "other-wavelet-id"));
    try {
      indexWaveletNameFor(invalid);
      fail("index wave wavelets shouldn't be convertable to index wave");
    } catch (IllegalArgumentException expected) {
      // pass
    }
  }

  /**
   * Tests that openRequest() yields no deltas if none are received via
   * waveletUpdate().
   */
  public void testInitialOpenRequestYieldsNothing() {
    OpenListener listener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, ALL_WAVELETS, Integer.MAX_VALUE, listener);
    verifyZeroInteractions(listener);
    clientFrontend.openRequest(USER, INDEX_WAVE_ID, ALL_WAVELETS, Integer.MAX_VALUE,
        listener);
    // we don't expect any invocations on the listener
    verifyZeroInteractions(listener);
  }

  /**
   * Tests that if our subscription doesn't involve a matching waveletIdPrefix,
   * deltas arriving via waveletUpdate(), aren't forwarded to the listener.
   */
  public void testDeltasArentPropagatedIfNotSubscribedToWavelet() {
    OpenListener listener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, ImmutableSet.of("nonexisting-wavelet"),
        Integer.MAX_VALUE, listener);
    clientFrontend.waveletUpdate(
        WAVELET_NAME, DELTAS, DELTAS.getEndVersion(), DOCUMENT_STATE);
    verifyZeroInteractions(listener);
  }

  /**
   * Test that clientFrontend.submitRequest() triggers
   * waveletProvider.submitRequest().
   */
  public void testSubmitGetsForwardedToWaveletProvider() {
    SubmitResultListener listener = mock(SubmitResultListener.class);
    clientFrontend.submitRequest(WAVELET_NAME, DELTA, listener);
    verify(waveletProvider).submitRequest(
        eq(WAVELET_NAME), eq(DELTA), (SubmitResultListener) isNotNull());
    verifyZeroInteractions(listener);
  }

  /**
   * Tests that an attempt to submit a delta to the index wave immediately
   * yields an expected failure message.
   */
  public void testCannotSubmitToIndexWave() {
    SubmitResultListener listener = mock(SubmitResultListener.class);

    clientFrontend.submitRequest(INDEX_WAVELET_NAME, DELTA, listener);
    verify(listener).onFailure(FederationErrors.badRequest("Wavelet " + INDEX_WAVELET_NAME
                                                           + " is readonly"));
  }

  /**
   * Tests that we get deltas if they arrive some time after we've opened
   * a subscription.
   */
  public void testOpenThenSendDeltas() {
    OpenListener listener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, ALL_WAVELETS, Integer.MAX_VALUE, listener);
    clientFrontend.participantUpdate(WAVELET_NAME, USER, DELTAS, true, false, "", "");
    verify(listener).onUpdate(WAVELET_NAME, DELTAS, DELTAS.getEndVersion());

  }

  /**
   * Tests that if we open the index wave, we don't get updates from the
   * original wave if they contain no interesting operations (add/remove
   * participant or text).
   */
  public void testOpenIndexThenSendDeltasNotOfInterest() {
    OpenListener listener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, INDEX_WAVE_ID, ALL_WAVELETS, Integer.MAX_VALUE, listener);
    List<? extends WaveletOperation> ops = ImmutableList.of(NoOp.INSTANCE);
    WaveletDelta delta = new WaveletDelta(USER, ops);
    DeltaSequence deltas = new DeltaSequence(
        ImmutableList.of(serialize(delta, VERSION_0)),
        serialize(HashedVersion.unsigned(1L)));
    clientFrontend.participantUpdate(WAVELET_NAME, USER, deltas, true, false, "", "");
    verifyZeroInteractions(listener);
  }

  /**
   * An OpenListener that expects only onUpdate() calls and publishes the
   * values passed in.
   */
  static final class UpdateListener implements OpenListener {
    WaveletName waveletName = null;
    DeltaSequence deltas = null;
    ProtocolHashedVersion endVersion = null;

    @Override
    public void onCommit(WaveletName wn, ProtocolHashedVersion commitNotice) {
      fail("unexpected");
    }

    @Override
    public void onFailure(String errorMessage) {
      fail("unexpected");
    }

    @Override
    public void onUpdate(WaveletName wn, List<ProtocolWaveletDelta> newDeltas,
        ProtocolHashedVersion endVersion) {
      assertNull(this.waveletName); // make sure we're not called twice
      this.waveletName = wn;
      this.deltas = new DeltaSequence(newDeltas, endVersion);
      this.endVersion = endVersion;
    }

    void clear() {
      assertNotNull(this.waveletName);
      this.waveletName = null;
      this.deltas = null;
      this.endVersion = null;
    }
  }

  private ProtocolWaveletDelta makeDelta(ParticipantId author, HashedVersion startVersion,
      WaveletOperation...operations) {
    WaveletDelta delta = new WaveletDelta(author, ImmutableList.of(operations));
    return serialize(delta, startVersion);
  }

  private void waveletUpdate(HashedVersion startVersion, Map<String, BufferedDocOp> documentState,
      WaveletOperation... operations) {
    ProtocolWaveletDelta delta = makeDelta(USER, startVersion, operations);
    DeltaSequence deltas = createUnsignedDeltas(ImmutableList.of(delta));
    clientFrontend.waveletUpdate(
        WAVELET_NAME, deltas, deltas.getEndVersion(), documentState);
  }

  private BufferedDocOp makeAppend(int retain, String text) {
    DocOpBuilder builder = new DocOpBuilder();
    if (retain > 0) {
      builder.retain(retain);
    }
    builder.characters(text);
    return builder.build();
  }

  private WaveletDocumentOperation makeAppendOp(String documentId, int retain, String text) {
    return new WaveletDocumentOperation(documentId, makeAppend(retain, text));
  }

  /**
   * Tests that a delta involving an addParticipant and a characters op
   * gets pushed through to the index wave as deltas that just summarise
   * the changes to the digest text and the participants, ignoring any
   * text from the first \n onwards.
   */
  public void testOpenIndexThenSendInterestingDeltas() {
    UpdateListener listener = new UpdateListener();
    clientFrontend.openRequest(USER, INDEX_WAVE_ID, ALL_WAVELETS, Integer.MAX_VALUE, listener);

    waveletUpdate(VERSION_0,
        ImmutableMap.of("default", makeAppend(0, "Hello, world\nignored text")),
        new AddParticipant(USER),
        NoOp.INSTANCE
        );

    assertEquals(INDEX_WAVELET_NAME, listener.waveletName);

    WaveletOperation helloWorldOp =
      makeAppendOp(DIGEST_DOCUMENT_ID, 0, "Hello, world");

    DeltaSequence expectedDeltas = createUnsignedDeltas(ImmutableList.of(
        makeDelta(DIGEST_AUTHOR, unsigned(0), helloWorldOp),
        makeDelta(USER, unsigned(1L), new AddParticipant(USER))));
    assertEquals(expectedDeltas, listener.deltas);
  }

  /**
   * Tests that when a subscription is added later than version 0, that listener
   * gets all previous deltas immediately, and both existing listeners and the
   * new listener get subsequent updates.
   */
  public void testOpenAfterVersionZero() {
    UpdateListener oldListener = new UpdateListener();
    clientFrontend.openRequest(USER, WAVE_ID, ALL_WAVELETS, Integer.MAX_VALUE, oldListener);
    Map<String, BufferedDocOp> documentState = Maps.newHashMap();

    BufferedDocOp addTextOp = makeAppend(0, "Hello, world");
    waveletUpdate(VERSION_0, documentState, new AddParticipant(USER),
        new WaveletDocumentOperation("docId", addTextOp));
    documentState.put("docId", addTextOp);

    assertTrue(!oldListener.deltas.isEmpty());

    // TODO(tobiast): Let requestHistory() return a DeltaSequence, and simplify this test
    NavigableSet<ProtocolWaveletDelta> expectedDeltas = new TreeSet<ProtocolWaveletDelta>(
        WaveletContainerImpl.transformedDeltaComparator);
    expectedDeltas.addAll(ImmutableList.copyOf(oldListener.deltas));
    ProtocolHashedVersion startVersion = serialize(HashedVersion.versionZero(WAVELET_NAME));
    when(waveletProvider.requestHistory(WAVELET_NAME, startVersion,
        oldListener.endVersion)).thenReturn(expectedDeltas);

    UpdateListener newListener = new UpdateListener();
    clientFrontend.openRequest(USER, WAVE_ID, ALL_WAVELETS, Integer.MAX_VALUE, newListener);
    // Upon subscription, newListener immediately got all the previous deltas
    assertEquals(oldListener.deltas, newListener.deltas);
    assertEquals(oldListener.endVersion, newListener.endVersion);

    when(waveletProvider.requestHistory(WAVELET_NAME,
        serialize(HashedVersion.versionZero(WAVELET_NAME)), oldListener.endVersion)).thenReturn(
        expectedDeltas
        );
    HashedVersion version = WaveletOperationSerializer.deserialize(oldListener.endVersion);
    oldListener.clear();
    newListener.clear();
    waveletUpdate(version, documentState,
        new AddParticipant(new ParticipantId("another-user")), NoOp.INSTANCE,
        new RemoveParticipant(USER));

    // Subsequent deltas go to both listeners
    assertEquals(oldListener.deltas, newListener.deltas);
    assertEquals(oldListener.endVersion, newListener.endVersion);
  }
}
