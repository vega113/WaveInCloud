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

package org.waveprotocol.box.server.waveserver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;

import junit.framework.TestCase;

import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.persistence.memory.MemoryDeltaStore;
import org.waveprotocol.box.server.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.box.server.waveserver.LocalWaveletContainer.Factory;
import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.wave.federation.Proto;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.waveserver.federation.WaveletFederationListener;
import org.waveprotocol.wave.waveserver.federation.WaveletFederationProvider;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class WaveServerTest extends TestCase {
  private static final WaveletId WAVELET_ID = new WaveletId("example.com", "conv+root");
  private static final WaveId WAVE_ID = new WaveId("example.com", "abc123");

  private static final ParticipantId USER1 = ParticipantId.ofUnsafe("user1@example.com");
  private static final ParticipantId USER2 = ParticipantId.ofUnsafe("user2@example.com");

  private static final WaveletOperationContext CONTEXT =
    new WaveletOperationContext(USER1, 1234567890, 1);
  
  @Mock private CertificateManager certificateManager;
  @Mock private WaveletFederationListener.Factory federationHostFactory;
  @Mock private WaveletFederationProvider federationRemote;
  @Mock private RemoteWaveletContainer.Factory remoteWaveletContainerFactory;

  private WaveServerImpl waveServer;
  private HashedVersionZeroFactoryImpl versionZeroFactory;

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    Set<String> domains = ImmutableSet.of("example.com");
    when(certificateManager.getLocalDomains()).thenReturn(domains);

    SignatureHandler localSigner = mock(SignatureHandler.class);
    when(localSigner.getSignerInfo()).thenReturn(null);
    when(certificateManager.getLocalSigner()).thenReturn(localSigner);
    final WaveletStore waveletStore = new DeltaStoreBasedWaveletStore(new MemoryDeltaStore());
    Factory localWaveletContainerFactory = new LocalWaveletContainer.Factory() {
      @Override
      public LocalWaveletContainer create(WaveletName waveletName) throws IOException {
        return new LocalWaveletContainerImpl(waveletStore.open(waveletName));
      }
    };

    waveServer = new WaveServerImpl(certificateManager, federationHostFactory, federationRemote,
        localWaveletContainerFactory, remoteWaveletContainerFactory);

    IdURIEncoderDecoder uriCodec =
        new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());
    versionZeroFactory = new HashedVersionZeroFactoryImpl(uriCodec);
  }

  public void testSearchEmptyInboxReturnsNothing() {
    Collection<WaveViewData> results = waveServer.search(USER1, "in:inbox", 0, 20);

    assertEquals(0, results.size());
  }

  public void testSearchInboxReturnsWaveWithExplicitParticipant() {
    WaveletName name = WaveletName.of(WAVE_ID, WAVELET_ID);
    submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER2));

    Collection<WaveViewData> results = waveServer.search(USER2, "in:inbox", 0, 20);

    assertEquals(1, results.size());
    assertEquals(name.waveId, results.iterator().next().getWaveId());
  }

  public void testSearchInboxDoesNotReturnWaveWithoutUser() {
    WaveletName name = WaveletName.of(WAVE_ID, WAVELET_ID);

    submitDeltaToNewWavelet(name, USER1,
        addParticipantToWavelet(USER1));

    Collection<WaveViewData> results = waveServer.search(USER2, "in:inbox", 0, 20);
    assertEquals(0, results.size());
  }

  public void testSearchLimitEnforced() {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(
          new WaveId("example.com", "w" + i), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1));
    }

    Collection<WaveViewData> results = waveServer.search(USER1, "in:inbox", 0, 5);

    assertEquals(5, results.size());
  }

  public void testSearchIndexWorks() {
    // For this test, we'll create 10 waves with wave ids "0", "1", ... "9" and then run 10
    // searches using offsets 0..9. The waves we get back can be in any order, but we must get
    // all 10 of the waves back exactly once each from the search query.

    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(
          new WaveId("example.com", String.valueOf(i)), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1));
    }

    // The number of times we see each wave when we search
    int[] saw_wave = new int[10];

    for (int i = 0; i < 10; i++) {
      Collection<WaveViewData> results = waveServer.search(USER1, "in:inbox", i, 1);
      assertEquals(1, results.size());
      int index = Integer.parseInt(results.iterator().next().getWaveId().getId());
      saw_wave[index]++;
    }

    for (int i = 0; i < 10; i++) {
      // Each wave should appear exactly once in the results
      assertEquals(1, saw_wave[i]);
    }
  }

  // *** Helpers

  private List<? extends WaveletOperation> addParticipantToWavelet(ParticipantId user) {
    return Arrays.asList(new AddParticipant(CONTEXT, user));
  }

  private void submitDeltaToNewWavelet(
      WaveletName name, ParticipantId user, List<? extends WaveletOperation> ops) {
    HashedVersion version = versionZeroFactory.createVersionZero(name);

    WaveletDelta delta = new WaveletDelta(user, version, ops);

    ProtocolWaveletDelta protoDelta = CoreWaveletOperationSerializer.serialize(delta);

    // Submitting the request will require the certificate manager to sign the delta. We'll just
    // leave it unsigned.
    ProtocolSignedDelta signedProtoDelta =
        ProtocolSignedDelta.newBuilder().setDelta(protoDelta.toByteString()).build();

    when(
        certificateManager.signDelta(Matchers.<ByteStringMessage<Proto.ProtocolWaveletDelta>>any()))
        .thenReturn(signedProtoDelta);
    waveServer.submitRequest(name, protoDelta, new SubmitRequestListener() {
      @Override
      public void onSuccess(int operationsApplied, HashedVersion hashedVersionAfterApplication,
          long applicationTimestamp) {
        // Wavelet was submitted.
      }

      @Override
      public void onFailure(String errorMessage) {
        fail("Could not submit callback");
      }
    });
  }
}
