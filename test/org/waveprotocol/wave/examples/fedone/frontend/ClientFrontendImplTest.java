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
package org.waveprotocol.wave.examples.fedone.frontend;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.waveprotocol.wave.examples.fedone.common.CommonConstants.INDEX_WAVE_ID;
import static org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer.deserialize;
import static org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer.serialize;
import static org.waveprotocol.wave.examples.fedone.frontend.ClientFrontendImpl.createUnsignedDeltas;
import static org.waveprotocol.wave.model.id.IdConstants.CONVERSATION_ROOT_WAVELET;

import com.google.common.collect.ImmutableList;
import com.google.inject.internal.Nullable;

import junit.framework.TestCase;

import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.waveprotocol.wave.examples.fedone.common.DeltaSequence;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactoryImpl;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.examples.fedone.common.SnapshotSerializer;
import org.waveprotocol.wave.examples.fedone.frontend.ClientFrontend.OpenListener;
import org.waveprotocol.wave.examples.fedone.util.WaveletDataUtil;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveBus;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletProvider;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.WaveletSnapshot;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.WaveletVersion;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.Proto;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.IdFilters;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreNoOp;
import org.waveprotocol.wave.model.operation.core.CoreRemoveParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.waveserver.federation.SubmitResultListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link ClientFrontendImpl}.
 */
public class ClientFrontendImplTest extends TestCase {
  private static final WaveId WAVE_ID = new WaveId("domain", "waveId");
  private static final WaveletId WAVELET_ID = new WaveletId("domain", CONVERSATION_ROOT_WAVELET);
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);
  private static final WaveletName INDEX_WAVELET_NAME =
      WaveletName.of(INDEX_WAVE_ID, new WaveletId("domain", "waveId"));
  private static final ParticipantId USER = new ParticipantId("user@host.com");
  private static final HashedVersion VERSION_0 =
      new HashedVersionZeroFactoryImpl().createVersionZero(WAVELET_NAME);
  private static final HashedVersion VERSION_1 = HashedVersion.unsigned(1L);
  private static final HashedVersion VERSION_2 = HashedVersion.unsigned(2L);
  private static final ProtocolWaveletDelta DELTA =
      serialize(new CoreWaveletDelta(USER, ImmutableList.of(new CoreAddParticipant(USER))),
      VERSION_0);
  private static final DeltaSequence DELTAS =
      new DeltaSequence(ImmutableList.of(DELTA), serialize(HashedVersion.unsigned(1L)));
  private static final Collection<WaveletVersion> NO_KNOWN_WAVELETS =
      Collections.<WaveletVersion>emptySet();

  private ClientFrontendImpl clientFrontend;
  private WaveletProvider waveletProvider;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    waveletProvider = mock(WaveletProvider.class);
    WaveBus waveBus = mock(WaveBus.class);
    this.clientFrontend = new ClientFrontendImpl(new HashedVersionFactoryImpl(), waveletProvider,
        waveBus);
    verify(waveBus).subscribe((WaveBus.Subscriber) isNotNull());
  }

  /**
   * Tests that openRequest() yields no deltas if none are received via
   * waveletUpdate().
   */
  public void testInitialOpenRequestYieldsNothing() {
    OpenListener listener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, listener);
    UserManager manager = clientFrontend.perUser.get(USER);

    WaveletName dummyWaveletName = ClientFrontendImpl.createDummyWaveletName(WAVE_ID);
    List<WaveViewSubscription> subscriptions = manager.matchSubscriptions(dummyWaveletName);
    assertEquals(1, subscriptions.size());

    verifyIfChannelIdAndMarkerSent(listener, dummyWaveletName, subscriptions.get(0).getChannelId());

    clientFrontend.openRequest(USER, INDEX_WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, listener);
    WaveletName indexDummyWaveletName = ClientFrontendImpl.createDummyWaveletName(INDEX_WAVE_ID);
    List<WaveViewSubscription> indexSubscriptions = manager.matchSubscriptions(indexDummyWaveletName);
    assertEquals(1, indexSubscriptions.size());
    verifyIfChannelIdAndMarkerSent(listener, indexDummyWaveletName,
        indexSubscriptions.get(0).getChannelId());
  }

  /**
   * Tests that if our subscription doesn't involve a matching waveletIdPrefix,
   * deltas arriving via waveletUpdate(), aren't forwarded to the listener.
   */
  public void testDeltasArentPropagatedIfNotSubscribedToWavelet() {
    OpenListener listener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, IdFilter.ofPrefixes("non-existing"),
        NO_KNOWN_WAVELETS, listener);

    UserManager manager = clientFrontend.perUser.get(USER);

    WaveletName dummyWaveletName = ClientFrontendImpl.createDummyWaveletName(WAVE_ID);
    List<WaveViewSubscription> subscriptions = manager.matchSubscriptions(dummyWaveletName);
    assertEquals(0, subscriptions.size());
    verifyIfChannelIdAndMarkerSent(listener, dummyWaveletName, null);

    WaveletData wavelet = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, USER, 0L);
    clientFrontend.waveletUpdate(wavelet, DELTAS.getEndVersion(), DELTAS);
    verify(listener, Mockito.never()).onUpdate(eq(WAVELET_NAME),
        any(WaveletSnapshotAndVersion.class), anyListOf(ProtocolWaveletDelta.class),
        any(Proto.ProtocolHashedVersion.class), any(Proto.ProtocolHashedVersion.class),
        anyBoolean(), anyString());
  }

  /**
   * Test that clientFrontend.submitRequest() triggers
   * waveletProvider.submitRequest().
   */
  public void testSubmitGetsForwardedToWaveletProvider() {
    SubmitResultListener listener = mock(SubmitResultListener.class);
    clientFrontend.submitRequest(WAVELET_NAME, DELTA, null /* channelid */, listener);
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

    clientFrontend.submitRequest(INDEX_WAVELET_NAME, DELTA, null /* channelid */, listener);
    verify(listener).onFailure(FederationErrors.badRequest("Wavelet " + INDEX_WAVELET_NAME
        + " is readonly"));
  }

  /**
   * Tests that we get deltas if they arrive some time after we've opened
   * a subscription.
   */
  public void testOpenThenSendDeltas() {
    OpenListener listener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, listener);
    clientFrontend.participantUpdate(WAVELET_NAME, USER, DELTAS, true, false, "", "");
    verify(listener).onUpdate(eq(WAVELET_NAME), isNullSnapshot(), eq(DELTAS),
        eq(DELTAS.getEndVersion()), isNullVersion(), eq(false), anyString());
  }

  /**
   * Tests that if we open the index wave, we don't get updates from the
   * original wave if they contain no interesting operations (add/remove
   * participant or text).
   */
  public void testOpenIndexThenSendDeltasNotOfInterest() {
    OpenListener listener = mock(OpenListener.class);
    clientFrontend.openRequest(USER, INDEX_WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, listener);
    UserManager manager = clientFrontend.perUser.get(USER);

    WaveletName dummyWaveletName = ClientFrontendImpl.createDummyWaveletName(INDEX_WAVE_ID);
    List<WaveViewSubscription> subscriptions = manager.matchSubscriptions(dummyWaveletName);
    assertEquals(1, subscriptions.size());
    verifyIfChannelIdAndMarkerSent(listener, dummyWaveletName, subscriptions.get(0).getChannelId());

    List<? extends CoreWaveletOperation> ops = ImmutableList.of(CoreNoOp.INSTANCE);
    CoreWaveletDelta delta = new CoreWaveletDelta(USER, ops);
    DeltaSequence deltas = new DeltaSequence(
        ImmutableList.of(serialize(delta, VERSION_0)),
        serialize(HashedVersion.unsigned(1L)));
    clientFrontend.participantUpdate(WAVELET_NAME, USER, deltas, true, false, "", "");

    verify(listener, Mockito.never()).onUpdate(eq(dummyWaveletName),
        any(WaveletSnapshotAndVersion.class), argThat(new IsNonEmptyList<ProtocolWaveletDelta>()),
        any(Proto.ProtocolHashedVersion.class), any(Proto.ProtocolHashedVersion.class),
        anyBoolean(), anyString());
  }

  private static class IsNonEmptyList<T> extends ArgumentMatcher<List<T>> {
    @Override
    @SuppressWarnings("unchecked")
    public boolean matches(Object list) {
      return list != null && ((List<T>)list).size() > 0;
    }
  }

  /**
   * An OpenListener that expects only onUpdate() calls and publishes the
   * values passed in.
   */
  static final class UpdateListener implements OpenListener {
    WaveletName waveletName = null;
    WaveletSnapshotAndVersion snapshot = null;
    DeltaSequence deltas = null;
    ProtocolHashedVersion endVersion = null;

    @Override
    public void onFailure(String errorMessage) {
      fail("unexpected");
    }

    @Override
    public void onUpdate(WaveletName wn,
        @Nullable WaveletSnapshotAndVersion snapshot,
        List<ProtocolWaveletDelta> newDeltas,
        @Nullable ProtocolHashedVersion endVersion,
        @Nullable ProtocolHashedVersion committedVersion, final boolean hasMarker,
        @Nullable final String channelId) {

      if (snapshot == null && newDeltas.isEmpty()) {
        // Ignore marker/channel id updates
        return;
      }

      assertNull(this.waveletName); // make sure we're not called twice
      assertNotNull(endVersion);
      // TODO(arb): check the committedVersion field correctly
      this.waveletName = wn;
      this.snapshot = snapshot;
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

  /**
   * Tests that a delta involving an addParticipant and a characters op
   * gets pushed through to the index wave as deltas that just summarise
   * the changes to the digest text and the participants, ignoring any
   * text from the first \n onwards.
   */
  public void testOpenIndexThenSendInterestingDeltas() throws OperationException {
    UpdateListener listener = new UpdateListener();
    clientFrontend.openRequest(USER, INDEX_WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, listener);

    WaveletData wavelet = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, USER, 0L);
    BlipData blip = WaveletDataUtil.addEmptyBlip(wavelet, "default", USER, 0L);
    blip.getContent().consume(makeAppend(0, "Hello, world\nignored text"));

    waveletUpdate(VERSION_0, HashedVersion.unsigned(2L), wavelet, new CoreAddParticipant(USER),
        CoreNoOp.INSTANCE);

    assertEquals(INDEX_WAVELET_NAME, listener.waveletName);

    CoreWaveletOperation expectedDigestOp =
        makeAppendOp(IndexWave.DIGEST_DOCUMENT_ID, 0, "Hello, world");

    DeltaSequence expectedDeltas = createUnsignedDeltas(ImmutableList.of(
        makeDelta(USER, HashedVersion.unsigned(0), HashedVersion.unsigned(1),
            new CoreAddParticipant(USER)),
        makeDelta(IndexWave.DIGEST_AUTHOR, HashedVersion.unsigned(1), HashedVersion.unsigned(2),
            expectedDigestOp)
        ));
    assertEquals(expectedDeltas, listener.deltas);
  }

  /**
   * Tests that when a subscription is added later than version 0, that listener
   * gets a snapshot and both existing listeners and the new listener get
   * subsequent updates.
   */
  public void testOpenAfterVersionZero() {
    UpdateListener oldListener = new UpdateListener();
    clientFrontend.openRequest(USER, WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, oldListener);

    WaveletData wavelet = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, USER, 0L);
    BufferedDocOp addTextOp = makeAppend(0, "Hello, world");
    waveletUpdate(VERSION_0, VERSION_1, wavelet, new CoreAddParticipant(USER),
        new CoreWaveletDocumentOperation("docId", addTextOp));
    assertFalse(oldListener.deltas.isEmpty());

    ProtocolHashedVersion startVersion = serialize(VERSION_0);
    WaveletSnapshot snapshot = SnapshotSerializer.serializeWavelet(wavelet, VERSION_2);
    WaveletSnapshotAndVersion snapshotAndV = new WaveletSnapshotAndVersion(snapshot, startVersion);
    when(waveletProvider.getSnapshot(eq(WAVELET_NAME))).thenReturn(snapshotAndV);

    UpdateListener newListener = new UpdateListener();
    clientFrontend.openRequest(USER, WAVE_ID, IdFilters.ALL_IDS, NO_KNOWN_WAVELETS, newListener);
    // Upon subscription, newListener got a snapshot.
    assertNotNull(newListener.snapshot);
    assertEquals(oldListener.endVersion, newListener.endVersion);

    HashedVersion endVersion = deserialize(oldListener.endVersion);
    oldListener.clear();
    newListener.clear();
    waveletUpdate(endVersion, HashedVersion.unsigned(endVersion.getVersion() + 3), wavelet,
        new CoreAddParticipant(new ParticipantId("another-user")), CoreNoOp.INSTANCE,
        new CoreRemoveParticipant(USER));

    // Subsequent deltas go to both listeners
    assertEquals(oldListener.deltas, newListener.deltas);
    assertEquals(oldListener.endVersion, newListener.endVersion);
  }

  private ProtocolWaveletDelta makeDelta(ParticipantId author, HashedVersion startVersion,
      HashedVersion endVersion,
      CoreWaveletOperation...operations) {
    CoreWaveletDelta delta = new CoreWaveletDelta(author, ImmutableList.copyOf(operations));
    return serialize(delta, startVersion);
  }

  private void waveletUpdate(HashedVersion startVersion, HashedVersion endVersion,
      WaveletData wavelet, CoreWaveletOperation... operations) {
    ProtocolWaveletDelta delta = makeDelta(USER, startVersion, endVersion, operations);
    DeltaSequence deltas = createUnsignedDeltas(ImmutableList.of(delta));
    clientFrontend.waveletUpdate(wavelet, deltas.getEndVersion(), deltas);
  }

  private BufferedDocOp makeAppend(int retain, String text) {
    DocOpBuilder builder = new DocOpBuilder();
    if (retain > 0) {
      builder.retain(retain);
    }
    builder.characters(text);
    return builder.build();
  }

  private CoreWaveletDocumentOperation makeAppendOp(String documentId, int retain, String text) {
    return new CoreWaveletDocumentOperation(documentId, makeAppend(retain, text));
  }

  private void verifyIfChannelIdAndMarkerSent(
      OpenListener listener, WaveletName dummyWaveletName, String channelId) {
    // First the channel id
    verify(listener).onUpdate(eq(dummyWaveletName), isNullSnapshot(),
        eq(new ArrayList<ProtocolWaveletDelta>()), isNullVersion(),
        isNullVersion(), eq(false), channelId == null ? anyString() : eq(channelId));
    // Secondly get the marker
    verify(listener).onUpdate(dummyWaveletName, null, new ArrayList<ProtocolWaveletDelta>(), null,
        null, true, null);
  }

  private static WaveletSnapshotAndVersion isNullSnapshot() {
    return (WaveletSnapshotAndVersion) Mockito.isNull();
  }

  private static ProtocolHashedVersion isNullVersion() {
    return (ProtocolHashedVersion) Mockito.isNull();
  }

}
