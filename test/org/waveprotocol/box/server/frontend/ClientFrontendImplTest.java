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
package org.waveprotocol.box.server.frontend;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.waveprotocol.box.common.CommonConstants.INDEX_WAVE_ID;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.common.IndexWave;
import org.waveprotocol.box.common.comms.WaveClientRpc.WaveletVersion;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.common.HashedVersionFactoryImpl;
import org.waveprotocol.box.server.frontend.ClientFrontend.OpenListener;
import org.waveprotocol.box.server.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.server.waveserver.WaveBus;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.IdFilters;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.testing.DeltaTestUtil;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link ClientFrontendImpl}.
 */
public class ClientFrontendImplTest extends TestCase {
  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());
  private static final HashedVersionFactory HASH_FACTORY =
      new HashedVersionFactoryImpl(URI_CODEC);

  private static final WaveId WAVE_ID = WaveId.of("domain", "waveId");
  private static final WaveletId WAVELET_ID =
    WaveletId.of("domain", IdConstants.CONVERSATION_ROOT_WAVELET);
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);
  private static final WaveletName INDEX_WAVELET_NAME =
      WaveletName.of(INDEX_WAVE_ID, WaveletId.of("domain", "waveId"));
  private static final ParticipantId USER = new ParticipantId("user@example.com");
  private static final DeltaTestUtil UTIL = new DeltaTestUtil(USER);
  private static final HashedVersion VERSION_0 = HASH_FACTORY.createVersionZero(WAVELET_NAME);
  private static final HashedVersion VERSION_1 = HashedVersion.unsigned(1L);
  private static final HashedVersion VERSION_2 = HashedVersion.unsigned(2L);
  private static final TransformedWaveletDelta DELTA = TransformedWaveletDelta.cloneOperations(
      USER, VERSION_1, 0, ImmutableList.of(UTIL.addParticipant(USER)));
  private static final DeltaSequence DELTAS = DeltaSequence.of(DELTA);
  private static final ProtocolWaveletDelta SERIALIZED_DELTA =
      CoreWaveletOperationSerializer.serialize(DELTA);
  private static final Collection<WaveletVersion> NO_KNOWN_WAVELETS =
      Collections.<WaveletVersion>emptySet();

  private ClientFrontendImpl clientFrontend;
  private WaveletProvider waveletProvider;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    waveletProvider = mock(WaveletProvider.class);
    WaveBus waveBus = mock(WaveBus.class);
    this.clientFrontend = new ClientFrontendImpl(HASH_FACTORY, waveletProvider, waveBus);
    verify(waveBus).subscribe((WaveBus.Subscriber) isNotNull());
  }

  public void testOpenEmptyWaveReceivesChannelIdAndMarker() {
    OpenListener listener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, listener);

    verifyChannelId(listener);
    verifyMarker(listener, WAVE_ID);
  }

  public void testTwoSubscriptionsReceiveDifferentChannelIds() {
    OpenListener listener1 = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, listener1);
    String ch1 = verifyChannelId(listener1);

    OpenListener listener2 = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, listener2);
    String ch2 = verifyChannelId(listener2);

    assertFalse(ch1.equals(ch2));
  }

  // Can't write this test yet as the frontend doesn't query for existing
  // wave state.
//  public void testRecieveSnapshotThenMarker() {
//    OpenListener listener = mock(OpenListener.class);
//    clientFrontend.openRequest(USER, WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, listener);
//    verifyChannelId(listener);
//
//    // ...
//
//    verifyMarker(listener, WAVE_ID);
//  }

  /**
   * Tests that a snapshot not matching the subscription filter is not received.
   */
  @SuppressWarnings("unchecked") // Mock container
  public void testUnsubscribedSnapshotNotRecieved() {
    OpenListener listener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, IdFilter.ofPrefixes("non-existing"),
        NO_KNOWN_WAVELETS, listener);
    verifyChannelId(listener);
    verifyMarker(listener, WAVE_ID);

    WaveletData wavelet = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, USER, VERSION_0, 0L);
    clientFrontend.waveletUpdate(wavelet, DELTAS);
    verify(listener, Mockito.never()).onUpdate(eq(WAVELET_NAME),
        any(CommittedWaveletSnapshot.class), Matchers.anyList(),
        any(HashedVersion.class), isNullMarker(), anyString());
  }

  /**
   * Tests that submit requests are forwarded to the wavelet provider.
   */
  public void testSubmitForwardedToWaveletProvider() {
    OpenListener openListener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, openListener);
    String channelId = verifyChannelId(openListener);

    SubmitRequestListener submitListener = mock(SubmitRequestListener.class);
    clientFrontend.submitRequest(USER, WAVELET_NAME, SERIALIZED_DELTA, channelId, submitListener);
    verify(waveletProvider).submitRequest(eq(WAVELET_NAME), eq(SERIALIZED_DELTA),
        any(SubmitRequestListener.class));
    verifyZeroInteractions(submitListener);
  }

  public void testCannotSubmitToIndexWave() {
    OpenListener openListener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, INDEX_WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS,
        openListener);
    String channelId = verifyChannelId(openListener);

    SubmitRequestListener submitListener = mock(SubmitRequestListener.class);
    clientFrontend.submitRequest(USER, INDEX_WAVELET_NAME, SERIALIZED_DELTA, channelId,
        submitListener);
    verify(submitListener).onFailure(anyString());
  }

  public void testCannotSubmitAsDifferentUser() {
    ParticipantId otherParticipant = new ParticipantId("another@example.com");
    OpenListener openListener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, openListener);
    String channelId = verifyChannelId(openListener);

    SubmitRequestListener submitListener = mock(SubmitRequestListener.class);
    clientFrontend.submitRequest(otherParticipant, WAVELET_NAME, SERIALIZED_DELTA, channelId,
        submitListener);
    verify(submitListener).onFailure(anyString());
    verify(submitListener, never()).onSuccess(anyInt(), (HashedVersion) any(), anyLong());
  }

  /**
   * Tests that we get deltas if they arrive some time after we've opened
   * a subscription.
   */
  public void testReceivedDeltasSentToClient() {
    OpenListener openListener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, openListener);
    String channelId = verifyChannelId(openListener);

    clientFrontend.participantUpdate(WAVELET_NAME, USER, DELTAS, true, false, "", "");
    verify(openListener).onUpdate(eq(WAVELET_NAME), isNullSnapshot(), eq(DELTAS),
        isNullVersion(), isNullMarker(), anyString());
  }

  public void testCannotOpenWavesWhenNotLoggedIn() {
    OpenListener listener = mock(OpenListener.class);
    clientFrontend.openRequest(null, WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, listener);
    clientFrontend.participantUpdate(WAVELET_NAME, USER, DELTAS, true, false, "", "");
    verify(listener).onFailure("Not logged in");
  }

  /**
   * Tests that if we open the index wave, we don't get updates from the
   * original wave if they contain no interesting operations (add/remove
   * participant or text).
   */
  public void testOpenIndexThenSendDeltasNotOfInterest() {
    OpenListener listener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, INDEX_WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, listener);
    verifyChannelId(listener);
    verifyMarker(listener, INDEX_WAVE_ID);

    HashedVersion v1 = HashedVersion.unsigned(1L);
    TransformedWaveletDelta delta = makeDelta(USER, v1, 0L, UTIL.noOp());
    DeltaSequence deltas = DeltaSequence.of(delta);
    clientFrontend.participantUpdate(WAVELET_NAME, USER, deltas, true, false, "", "");

    WaveletName dummyWaveletName = ClientFrontendImpl.createDummyWaveletName(INDEX_WAVE_ID);
    verify(listener, Mockito.never()).onUpdate(eq(dummyWaveletName),
        any(CommittedWaveletSnapshot.class),
        isDeltasStartingAt(0),
        any(HashedVersion.class), isNullMarker(), anyString());
  }

  /**
   * Tests that a delta involving an addParticipant and a characters op
   * gets pushed through to the index wave as deltas that just summarise
   * the changes to the digest text and the participants, ignoring any
   * text from the first \n onwards.
   */
  public void testOpenIndexThenSendInterestingDeltas() throws OperationException {
    OpenListener listener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, INDEX_WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, listener);
    String channelId = verifyChannelId(listener);
    verifyMarker(listener, INDEX_WAVE_ID);

    WaveletData wavelet = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, USER, VERSION_0, 0L);
    BlipData blip = WaveletDataUtil.addEmptyBlip(wavelet, "default", USER, 0L);
    blip.getContent().consume(makeAppend(0, "Hello, world\nignored text"));

    HashedVersion v2 = HashedVersion.unsigned(2L);
    waveletUpdate(v2, 0L, wavelet, UTIL.addParticipant(USER), UTIL.noOp());
    verify(listener).onUpdate(eq(INDEX_WAVELET_NAME), isNullSnapshot(), isDeltasStartingAt(0),
        isNullVersion(), isNullMarker(), eq(channelId));

    WaveletOperation expectedDigestOp =
        makeAppendOp(IndexWave.DIGEST_DOCUMENT_ID, 0, "Hello, world", new WaveletOperationContext(
            IndexWave.DIGEST_AUTHOR, 0L, 1, v2));

    HashedVersion v1 = HashedVersion.unsigned(1);
    DeltaSequence expectedDeltas = DeltaSequence.of(
        makeDelta(USER, v1, 0L, UTIL.addParticipant(USER)),
        makeDelta(IndexWave.DIGEST_AUTHOR, v2, 0L, expectedDigestOp));
    verify(listener).onUpdate(eq(INDEX_WAVELET_NAME), isNullSnapshot(), eq(expectedDeltas),
        isNullVersion(), isNullMarker(), eq(channelId));
  }

  /**
   * Tests that when a subscription is added later than version 0, that listener
   * gets a snapshot and both existing listeners and the new listener get
   * subsequent updates.
   */
  public void testOpenAfterVersionZero() throws Exception {
    OpenListener oldListener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, oldListener);
    String oldChannelId = verifyChannelId(oldListener);
    verifyMarker(oldListener, WAVE_ID);

    WaveletData wavelet = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, USER, VERSION_0, 0L);
    waveletUpdate(VERSION_2, 0L, wavelet, UTIL.addParticipant(USER),
        UTIL.noOpDocOp("docId"));
    verify(oldListener).onUpdate(eq(WAVELET_NAME), isNullSnapshot(), isDeltasStartingAt(0),
        isNullVersion(), isNullMarker(), eq(oldChannelId));

    CommittedWaveletSnapshot snapshotAndV = new CommittedWaveletSnapshot(wavelet, VERSION_0);
    when(waveletProvider.getSnapshot(eq(WAVELET_NAME))).thenReturn(snapshotAndV);

    OpenListener newListener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, newListener);
    verify(newListener).onUpdate(eq(WAVELET_NAME), eq(snapshotAndV), eq(DeltaSequence.empty()),
        any(HashedVersion.class), eq(false), any(String.class));

    HashedVersion v5 = HashedVersion.unsigned(5);
    waveletUpdate(v5, 0L, wavelet,
        UTIL.addParticipant(new ParticipantId("another-user")),
        UTIL.noOp(),
        UTIL.removeParticipant(USER));

    // Subsequent deltas go to both listeners
    verify(newListener).onUpdate(eq(WAVELET_NAME), isNullSnapshot(), isDeltasStartingAt(2),
        isNullVersion(), isNullMarker(), any(String.class));
    verify(oldListener).onUpdate(eq(WAVELET_NAME), isNullSnapshot(), isDeltasStartingAt(2),
        isNullVersion(), isNullMarker(), eq(oldChannelId));
  }

  private TransformedWaveletDelta makeDelta(ParticipantId author, HashedVersion endVersion,
      long timestamp, WaveletOperation... operations) {
    return TransformedWaveletDelta.cloneOperations(author, endVersion, timestamp,
        Arrays.asList(operations));
  }

  private void waveletUpdate(HashedVersion endVersion, long timestamp,
      WaveletData wavelet, WaveletOperation... operations) {
    TransformedWaveletDelta delta = makeDelta(USER, endVersion, timestamp, operations);
    DeltaSequence deltas = DeltaSequence.of(delta);
    clientFrontend.waveletUpdate(wavelet, deltas);
  }

  private DocOp makeAppend(int retain, String text) {
    DocOpBuilder builder = new DocOpBuilder();
    if (retain > 0) {
      builder.retain(retain);
    }
    builder.characters(text);
    return builder.build();
  }

  private WaveletBlipOperation makeAppendOp(String documentId, int retain, String text,
      WaveletOperationContext context) {
    return new WaveletBlipOperation(documentId,
        new BlipContentOperation(context, makeAppend(retain, text)));
  }

  /**
   * Verifies that the listener received a channel id.
   *
   * @return the channel id received
   */
  private String verifyChannelId(OpenListener listener) {
    ArgumentCaptor<String> channelIdCaptor = ArgumentCaptor.forClass(String.class);
    verify(listener).onUpdate(any(WaveletName.class), isNullSnapshot(), eq(DeltaSequence.empty()),
        isNullVersion(), isNullMarker(), channelIdCaptor.capture());
    return channelIdCaptor.getValue();
  }

  /**
   * Verifies that the listener received a marker.
   */
  private void verifyMarker(OpenListener listener, WaveId waveId) {
    ArgumentCaptor<WaveletName> waveletNameCaptor = ArgumentCaptor.forClass(WaveletName.class);
    verify(listener).onUpdate(waveletNameCaptor.capture(), isNullSnapshot(),
        eq(DeltaSequence.empty()), isNullVersion(), eq(true), (String) Mockito.isNull());
    assertEquals(waveId, waveletNameCaptor.getValue().waveId);
  }

  private static CommittedWaveletSnapshot isNullSnapshot() {
    return (CommittedWaveletSnapshot) Mockito.isNull();
  }

  private static HashedVersion isNullVersion() {
    return (HashedVersion) Mockito.isNull();
  }

  private static Boolean isNullMarker() {
    return (Boolean) Mockito.isNull();
  }

  private static String isNullString() {
    return (String) Mockito.isNull();
  }

  private static List<TransformedWaveletDelta> isDeltasStartingAt(final long version) {
    return argThat(new ArgumentMatcher<List<TransformedWaveletDelta>>() {
      @Override
      public boolean matches(Object sequence) {
        if (sequence != null) {
          DeltaSequence s = (DeltaSequence) sequence;
          return (s.size() > 0) && (s.getStartVersion() == version);
        }
        return false;
      }
    });
  }
}
